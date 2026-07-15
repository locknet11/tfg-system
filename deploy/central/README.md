# TFG Central Platform (Terraform, AWS)

Deploys the control plane — **API + UI + Caddy** (auto-HTTPS) — on a single `t3.small`,
using **MongoDB Atlas** as the database (not deployed on the VM). Separate from the
`deploy/aws-lab` module; its own state and `terraform.tfvars`.

## What it builds

- One `t3.small` Ubuntu 22.04 instance with an **Elastic IP** (stable address for DNS/certs).
- Docker + a `docker compose` stack: `api`, `ui`, `caddy` (Caddy terminates TLS and reverse-proxies
  `ui_domain → ui:80` and `api_domain → api:8080`).
- Security group: **80/443 open to the internet** (dashboard, API, agent report-back, Let's Encrypt),
  **22 restricted to `admin_cidr`**.
- A systemd unit so the stack restarts on reboot.

The api/ui images are pulled from your public registry (no token). The compose + Caddyfile are
rendered by Terraform from your variables and shipped inside cloud-init (no S3 needed).

## Prerequisites

1. **MongoDB Atlas**: a cluster + connection string. Add this instance's public IP (the Elastic IP
   from `terraform output public_ip`) to **Atlas → Network Access**.
2. **DNS**: after `apply`, create **A records** for `ui_domain` and `api_domain` pointing at
   `terraform output public_ip`. Caddy can only issue certificates once DNS resolves here.
3. AWS credentials, an SSH key, and your workstation IP.

## Usage

```bash
cd deploy/central
cp terraform.tfvars.example terraform.tfvars
# edit: ssh_public_key, admin_cidr, ui_domain, api_domain, mongodb_uri

terraform init
terraform plan
terraform apply

terraform output          # public IP, DNS records to create, URLs
```

Then:
1. Point DNS (`dns_records_required` output) at the Elastic IP.
2. Add the Elastic IP to Atlas Network Access.
3. Wait a couple of minutes — watch bootstrap + cert issuance:
   ```bash
   ssh ubuntu@<ip> 'sudo tail -f /var/log/central-bootstrap.log'
   ssh ubuntu@<ip> 'cd /opt/central && sudo docker compose ps && sudo docker compose logs -f caddy'
   ```
4. Open `https://<ui_domain>` and create the initial admin via the app's setup flow.

## Notes

- **JWT secret**: leave `jwt_secret` empty to auto-generate a stable one (kept in state), or pin your
  own. Changing it invalidates existing tokens.
- **Elastic IP**: keeps the address stable across stop/start (unlike the lab module's auto-assigned
  IPs). Free while attached to a running instance.
- **Fresh database**: Atlas starts empty for this deploy — you'll create the admin user and register
  org/project/targets in the app. (The `deploy/aws-lab` targets are what you register here.)
- **Config changes** (domains, secrets, image tags): edit `terraform.tfvars` and `terraform apply`.
  Changing `user_data` replaces the instance; to just restart the stack, `ssh` in and
  `cd /opt/central && sudo docker compose up -d`.

## Tear down

```bash
terraform destroy
```
