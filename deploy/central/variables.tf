variable "region" {
  description = "AWS region."
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Name prefix + tag for all resources."
  type        = string
  default     = "tfg-central"
}

variable "ssh_public_key" {
  description = "SSH public key contents used to reach the instance."
  type        = string
}

variable "admin_cidr" {
  description = "Your public IP in CIDR form for SSH access (e.g. \"203.0.113.7/32\"). curl -s ifconfig.me"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type. t3.small (2 GiB) recommended for the JVM API + Mongo + UI + Caddy."
  type        = string
  default     = "t3.small"
}

# --- Domains (Caddy issues Let's Encrypt certs for these; point their DNS A records at this instance) ---
variable "ui_domain" {
  description = "Domain served for the dashboard UI."
  type        = string
  default     = "tfg.locknet.com.ar"
}

variable "api_domain" {
  description = "Domain served for the API."
  type        = string
  default     = "tfg-api.locknet.com.ar"
}

# --- MongoDB Atlas (managed; not deployed on the VM) ---
variable "mongodb_uri" {
  description = "MongoDB Atlas connection string, e.g. mongodb+srv://user:pass@cluster.xxx.mongodb.net/?retryWrites=true&w=majority. Add this instance's IP to the Atlas Network Access allowlist."
  type        = string
  sensitive   = true
}

variable "mongodb_database_name" {
  description = "MongoDB database name."
  type        = string
  default     = "tfg-system"
}

# --- Secrets / integration (leave empty to disable a feature; JWT is auto-generated if empty) ---
variable "jwt_secret" {
  description = "JWT signing secret. Leave empty to auto-generate a stable random one."
  type        = string
  default     = ""
  sensitive   = true
}

variable "resend_api_key" {
  description = "Resend API key for email (optional)."
  type        = string
  default     = ""
  sensitive   = true
}

variable "resend_from_address" {
  description = "Resend From address (optional)."
  type        = string
  default     = ""
}

variable "nvd_api_key" {
  description = "NVD API key for CVE lookups (optional; higher rate limit)."
  type        = string
  default     = ""
  sensitive   = true
}

variable "central_public_key" {
  description = "Central public key for agent replication (optional)."
  type        = string
  default     = ""
  sensitive   = true
}

variable "replication_private_key" {
  description = "Replication private key for agent replication (optional)."
  type        = string
  default     = ""
  sensitive   = true
}

# --- Images (public; no pull token required) ---
variable "api_image" {
  description = "API container image."
  type        = string
  default     = "registry.locknet.com.ar/tfg/api"
}

variable "ui_image" {
  description = "UI container image."
  type        = string
  default     = "registry.locknet.com.ar/tfg/ui"
}

variable "allocate_eip" {
  description = "Allocate a stable Elastic IP (recommended so DNS/cert survive stop/start). Free while attached to a running instance."
  type        = bool
  default     = true
}

variable "root_volume_gb" {
  description = "Root EBS size (GiB) — holds the container images."
  type        = number
  default     = 20
}

variable "swap_gb" {
  description = "Swapfile size (GiB) as headroom for the JVM + Mongo."
  type        = number
  default     = 2
}
