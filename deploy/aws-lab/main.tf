########################################
# Networking + AMI (uses the default VPC)
########################################

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Canonical's Ubuntu 22.04 LTS, amd64 (x86_64 — matches the linux-x86_64 agent binary).
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"]

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_key_pair" "lab" {
  key_name   = "${var.project}-key"
  public_key = var.ssh_public_key
}

########################################
# Ship the lab/ directory via S3 (too big for user-data)
########################################

resource "random_id" "suffix" {
  byte_length = 4
}

data "archive_file" "lab" {
  type        = "zip"
  source_dir  = "${path.module}/../../lab"
  output_path = "${path.module}/.build/lab.zip"
  excludes    = ["node_modules", "**/__pycache__", "**/*.pyc"]
}

resource "aws_s3_bucket" "lab" {
  bucket        = "${var.project}-${random_id.suffix.hex}"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "lab" {
  bucket                  = aws_s3_bucket.lab.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_object" "lab" {
  bucket = aws_s3_bucket.lab.id
  key    = "lab.zip"
  source = data.archive_file.lab.output_path
  etag   = data.archive_file.lab.output_md5
}

########################################
# IAM: instances may read only the lab bucket
########################################

data "aws_iam_policy_document" "assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "lab" {
  name               = "${var.project}-role"
  assume_role_policy = data.aws_iam_policy_document.assume.json
}

data "aws_iam_policy_document" "bucket_read" {
  statement {
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.lab.arn}/*"]
  }
  statement {
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.lab.arn]
  }
}

resource "aws_iam_role_policy" "bucket_read" {
  name   = "${var.project}-bucket-read"
  role   = aws_iam_role.lab.id
  policy = data.aws_iam_policy_document.bucket_read.json
}

resource "aws_iam_instance_profile" "lab" {
  name = "${var.project}-profile"
  role = aws_iam_role.lab.name
}

# SSM Session Manager: lets you open a shell on any lab host via `aws ssm start-session`
# from any IP (out-of-band, through the ssm-agent's outbound channel — not sshd), so agent
# installs work while travelling without ever widening the intentionally-vulnerable lab SGs.
# Does NOT touch the vulnerable OpenSSH package: regreSSHion stays present for detect/remediate.
resource "aws_iam_role_policy_attachment" "lab_ssm" {
  role       = aws_iam_role.lab.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

########################################
# Security groups (locked to admin + central only)
########################################

locals {
  allowed_cidrs = distinct([var.admin_cidr, var.central_cidr])
}

resource "aws_security_group" "docker_lab" {
  name        = "${var.project}-docker-lab-sg"
  description = "SSH + lab service ports, restricted to admin and central."
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = local.allowed_cidrs
  }

  dynamic "ingress" {
    for_each = toset(var.lab_ingress_ports)
    content {
      description = "lab service ${ingress.value}"
      from_port   = ingress.value
      to_port     = ingress.value
      protocol    = "tcp"
      cidr_blocks = local.allowed_cidrs
    }
  }

  ingress {
    description = "bind9 DNS (udp)"
    from_port   = 5353
    to_port     = 5353
    protocol    = "udp"
    cidr_blocks = local.allowed_cidrs
  }

  egress {
    description = "all outbound (agent report-back, package downloads)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "target_vm" {
  name        = "${var.project}-target-vm-sg"
  description = "SSH + configured target service ports, restricted to admin and central."
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = local.allowed_cidrs
  }

  dynamic "ingress" {
    for_each = { for s in var.target_vm_services : s.name => s.host_port }
    content {
      description = "target service ${ingress.key}"
      from_port   = ingress.value
      to_port     = ingress.value
      protocol    = "tcp"
      cidr_blocks = local.allowed_cidrs
    }
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

########################################
# Instances
########################################

resource "aws_instance" "docker_lab" {
  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.docker_lab_instance_type
  subnet_id                   = data.aws_subnets.default.ids[0]
  vpc_security_group_ids      = [aws_security_group.docker_lab.id]
  key_name                    = aws_key_pair.lab.key_name
  iam_instance_profile        = aws_iam_instance_profile.lab.name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/templates/docker-lab.sh.tftpl", {
    bucket  = aws_s3_bucket.lab.id
    lab_key = aws_s3_object.lab.key
    region  = var.region
    swap_gb = var.swap_gb
  })

  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  tags = { Name = "${var.project}-docker-lab" }
}

resource "aws_instance" "target_vm" {
  count                       = var.target_vm_count
  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.target_vm_instance_type
  subnet_id                   = element(data.aws_subnets.default.ids, count.index)
  vpc_security_group_ids      = [aws_security_group.target_vm.id]
  key_name                    = aws_key_pair.lab.key_name
  iam_instance_profile        = aws_iam_instance_profile.lab.name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/templates/target-vm.sh.tftpl", {
    services        = var.target_vm_services
    swap_gb         = var.swap_gb
    openssh_version = var.openssh_vulnerable_version
  })

  root_block_device {
    volume_size = 12
    volume_type = "gp3"
  }

  tags = { Name = "${var.project}-target-${count.index + 1}" }
}
