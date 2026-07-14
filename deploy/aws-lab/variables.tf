variable "region" {
  description = "AWS region to deploy the lab in."
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Name prefix + tag applied to every resource."
  type        = string
  default     = "tfg-vuln-lab"
}

variable "ssh_public_key" {
  description = "Contents of the SSH public key used to reach the instances (e.g. file(\"~/.ssh/id_ed25519.pub\"))."
  type        = string
}

variable "admin_cidr" {
  description = "Your workstation's public IP in CIDR form (e.g. \"203.0.113.7/32\"). Get it with: curl -s ifconfig.me"
  type        = string
}

variable "central_cidr" {
  description = "The control-plane VPS public IP in CIDR form (e.g. \"198.51.100.9/32\") so the central API/agents can reach the targets. Set equal to admin_cidr if the central API runs on your workstation."
  type        = string
}

variable "docker_lab_instance_type" {
  description = "Instance type for the host running the full docker lab. t3.micro (2 GiB) recommended over t2.micro (1 GiB) to avoid OOM under load."
  type        = string
  default     = "t3.micro"
}

variable "target_vm_instance_type" {
  description = "Instance type for the standalone target VMs."
  type        = string
  default     = "t2.micro"
}

variable "target_vm_count" {
  description = "How many standalone target VMs to create (in addition to the docker-lab host)."
  type        = number
  default     = 2
}

variable "openssh_vulnerable_version" {
  description = "OpenSSH version installed on each target VM's HOST so the agent can detect + remediate it. 1:8.9p1-3 is the frozen jammy GA build, vulnerable to CVE-2024-6387 (regreSSHion); the platform remediates it by upgrading to the patched build. This is what makes the VMs a real, remediable target (unlike the docker lab, where remediation is skipped)."
  type        = string
  default     = "1:8.9p1-3"
}

variable "target_vm_services" {
  description = "OPTIONAL extra containers run on EACH target VM (host_port opens in the security group). Empty by default; the primary payload is the host-level vulnerable OpenSSH. Note: containerized services get remediation-skipped by the agent."
  type = list(object({
    name           = string
    host_port      = number
    container_port = number
    image          = string
  }))
  default = []
}

variable "swap_gb" {
  description = "Swapfile size (GiB) added to every instance as headroom for exploitation/remediation spikes."
  type        = number
  default     = 2
}

# Ports opened on the docker-lab host (matches lab/docker-compose.yml). Restricted to admin_cidr + central_cidr.
variable "lab_ingress_ports" {
  description = "TCP ports to expose on the docker-lab host."
  type        = list(number)
  default     = [8081, 8082, 8000, 8083, 2375, 8888, 5432, 3306, 2525, 9000, 3000]
}
