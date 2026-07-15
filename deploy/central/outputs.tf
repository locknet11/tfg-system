output "public_ip" {
  description = "Public IP of the central host (Elastic IP if allocated). Point your DNS A records here."
  value       = local.public_ip
}

output "dns_records_required" {
  description = "DNS A records to create before Caddy can issue certificates."
  value = {
    (var.ui_domain)  = local.public_ip
    (var.api_domain) = local.public_ip
  }
}

output "dashboard_url" {
  description = "Dashboard URL (works once DNS points here and Caddy has issued a cert)."
  value       = "https://${var.ui_domain}"
}

output "api_url" {
  description = "API URL (what agents and the UI call)."
  value       = "https://${var.api_domain}"
}

output "ssh" {
  description = "SSH command for the central host."
  value       = "ssh ubuntu@${local.public_ip}"
}

output "jwt_secret_generated" {
  description = "True if a JWT secret was auto-generated (set jwt_secret to pin it)."
  value       = nonsensitive(var.jwt_secret == "")
}
