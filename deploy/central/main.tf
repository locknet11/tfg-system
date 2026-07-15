########################################
# Networking + AMI (default VPC)
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

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_key_pair" "central" {
  key_name   = "${var.project}-key"
  public_key = var.ssh_public_key
}

########################################
# JWT secret (auto-generate a stable one if not provided)
########################################

resource "random_id" "jwt" {
  byte_length = 32
}

# Replication signing keypair (RSA). The API requires REPLICATION_PRIVATE_KEY (PKCS#8) and
# CENTRAL_PUBLIC_KEY. Auto-generated unless overridden via variables. Passed as single-line
# base64 (headers/newlines stripped — the app strips them too and base64-decodes).
resource "tls_private_key" "replication" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

locals {
  jwt_secret = var.jwt_secret != "" ? var.jwt_secret : random_id.jwt.b64_std

  # A trailing slash in the URI would make the app build "...net//db" (databaseName "/db").
  mongodb_uri = trimsuffix(var.mongodb_uri, "/")

  repl_private_key = var.replication_private_key != "" ? var.replication_private_key : replace(replace(replace(
  tls_private_key.replication.private_key_pem_pkcs8, "-----BEGIN PRIVATE KEY-----", ""), "-----END PRIVATE KEY-----", ""), "\n", "")

  repl_public_key = var.central_public_key != "" ? var.central_public_key : replace(replace(replace(
  tls_private_key.replication.public_key_pem, "-----BEGIN PUBLIC KEY-----", ""), "-----END PUBLIC KEY-----", ""), "\n", "")
}

########################################
# Security group: 80/443 public (dashboard, API, agents, ACME), 22 admin-only
########################################

resource "aws_security_group" "central" {
  name        = "${var.project}-sg"
  description = "Public 80/443 for the platform + Caddy ACME; SSH restricted to admin."
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP (Caddy ACME challenge + redirect)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS (dashboard, API, agent report-back)"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.admin_cidr]
  }

  egress {
    description = "all outbound (image pulls, MongoDB Atlas, ACME/TLS, NVD)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

########################################
# Instance
########################################

resource "aws_instance" "central" {
  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.instance_type
  subnet_id                   = data.aws_subnets.default.ids[0]
  vpc_security_group_ids      = [aws_security_group.central.id]
  key_name                    = aws_key_pair.central.key_name
  associate_public_ip_address = true

  user_data = templatefile("${path.module}/templates/bootstrap.sh.tftpl", {
    swap_gb = var.swap_gb
    compose_b64 = base64encode(templatefile("${path.module}/templates/docker-compose.yml.tftpl", {
      api_image               = var.api_image
      ui_image                = var.ui_image
      mongodb_uri             = local.mongodb_uri
      mongodb_database_name   = var.mongodb_database_name
      jwt_secret              = local.jwt_secret
      ui_domain               = var.ui_domain
      api_domain              = var.api_domain
      resend_api_key          = var.resend_api_key
      resend_from_address     = var.resend_from_address
      nvd_api_key             = var.nvd_api_key
      central_public_key      = local.repl_public_key
      replication_private_key = local.repl_private_key
    }))
    caddyfile_b64 = base64encode(templatefile("${path.module}/templates/Caddyfile.tftpl", {
      ui_domain  = var.ui_domain
      api_domain = var.api_domain
    }))
  })

  root_block_device {
    volume_size = var.root_volume_gb
    volume_type = "gp3"
  }

  tags = { Name = "${var.project}" }
}

########################################
# Elastic IP (stable address for DNS / certs; free while attached to a running instance)
########################################

resource "aws_eip" "central" {
  count    = var.allocate_eip ? 1 : 0
  instance = aws_instance.central.id
  domain   = "vpc"
  tags     = { Name = "${var.project}-eip" }
}

locals {
  public_ip = var.allocate_eip ? aws_eip.central[0].public_ip : aws_instance.central.public_ip
}
