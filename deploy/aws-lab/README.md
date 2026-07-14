# AWS Vulnerable Lab (Terraform)

Reproducible AWS lab of **deliberately vulnerable** target hosts for the thesis demo:
the central platform's agent scans and exploits/remediates them. Stand it up, test,
`destroy`, and re-`apply` for a clean lab on demo day.

> ⚠️ **These hosts are intentionally exploitable.** Every ingress rule is locked to
> `admin_cidr` + `central_cidr` (both `/32`). Never widen to `0.0.0.0/0`, and run
> `terraform destroy` as soon as the demo is over.

## What it builds

- **1 docker-lab host** (`t3.micro`, 2 GiB) — runs the full `../../lab/docker-compose.yml`
  (Drupal, Tomcat, Flask, ThinkPHP, the privileged Docker-API target on 2375/8888,
  Postgres, MySQL, bind9, Postfix, php-fpm, nodejs). This is the container-exploitation /
  plan-execution target.
- **N target VMs** (`t2.micro`, default 2) — each installs a **host-level vulnerable
  OpenSSH** (`1:8.9p1-3`, CVE-2024-6387 "regreSSHion"). These are real hosts the agent
  can **detect and remediate** (the platform's `apt-get install -y openssh-server`
  upgrades them to the patched build) — this is the full-cycle-with-remediation target,
  as opposed to the docker lab where remediation is container-skipped. Optionally also
  runs extra vulnerable containers via `target_vm_services` (default none).
- Supporting: default-VPC security groups, an SSH key pair, and a private S3 bucket +
  IAM instance profile used only to ship the `lab/` bundle to the docker-lab host on boot.

amd64 (Ubuntu 22.04) is used so the `linux-x86_64` agent binary matches.

## Prerequisites

- Terraform ≥ 1.5, AWS credentials (`aws configure` or env vars) with EC2 + S3 + IAM rights.
- An SSH key pair. Your workstation's public IP and the control-plane VPS IP.

## Usage

```bash
cd deploy/aws-lab
cp terraform.tfvars.example terraform.tfvars
# edit terraform.tfvars: ssh_public_key, admin_cidr (curl -s ifconfig.me), central_cidr

terraform init
terraform plan
terraform apply

terraform output            # IPs, SSH commands, service URLs
```

First boot installs Docker and builds the lab images, so services take a few minutes to
come up. Watch progress on a host:

```bash
ssh ubuntu@<ip> 'sudo tail -f /var/log/lab-bootstrap.log'      # docker-lab host
ssh ubuntu@<ip> 'sudo tail -f /var/log/target-bootstrap.log'   # target VMs
```

Register the IPs from `terraform output all_target_ips` as targets in the platform.

## Verify + re-arm (helper scripts)

After `apply`, sanity-check the whole lab in one command (nothing is changed):

```bash
export SSH_KEY=~/.ssh/your_key        # optional; else your default SSH key/agent
./scripts/verify-lab.sh
```

It reports PASS/FAIL for: docker-lab bootstrap + all containers up + exploitability probes
(docker-api unauth, flask SSTI, service reachability), **external** reachability from your
machine (catches security-group / `admin_cidr` mistakes — the #1 demo-day failure), and each
target VM's sshd + whether OpenSSH is in the **VULNERABLE** state and ready to demo.

Re-arm a target after a rehearsal (the agent upgraded/patched it) — downgrades OpenSSH back
to vulnerable **without recreating the instance**, so you can re-run the remediation demo:

```bash
./scripts/reset-target.sh             # all target VMs
./scripts/reset-target.sh 1.2.3.4     # a specific one
```

**Rehearsal loop:** `terraform apply` → `verify-lab.sh` → run the agent demo →
`reset-target.sh` → repeat.

## Troubleshooting (fast)

| Symptom | Likely cause / fix |
|---|---|
| `verify-lab.sh` SSH fails | Your IP changed → update `admin_cidr` (`curl -s ifconfig.me`) and `terraform apply` (SG-only, no rebuild) |
| Reachable on-host but "NOT reachable from here" | Security group / `admin_cidr` or `central_cidr` doesn't include the caller |
| Lab containers < 11 | Bootstrap still building or a build failed → `ssh ubuntu@<ip> 'sudo tail -50 /var/log/lab-bootstrap.log'` |
| Target OpenSSH shows patched version | Already remediated (or auto-patched) → `./scripts/reset-target.sh <ip>` |
| Agent scans but no remediation runs | Confirm the target is the **VM** (host OpenSSH), not the docker lab (containers are skipped by design) |

## Tear down / recreate

```bash
terraform destroy     # after testing
terraform apply       # clean lab again on demo day
```

State is local (`terraform.tfstate`) and gitignored along with `terraform.tfvars`.

## Common knobs

| Variable | Purpose | Default |
|---|---|---|
| `docker_lab_instance_type` | Lab host size | `t3.micro` |
| `target_vm_count` | Number of standalone VMs | `2` |
| `target_vm_services` | Containers per target VM | drupal + thinkphp |
| `swap_gb` | Swapfile per host | `2` |
| `lab_ingress_ports` | Ports opened on the lab host | full lab set |

## Notes / gotchas

- The docker-lab `docker-api` target is **privileged and exposes 2375** — effectively
  host-root for anyone who reaches it. The `/32` allow-list is what keeps that safe.
- If your workstation IP changes, update `admin_cidr` and `terraform apply` (SG update
  only, no rebuild).
- The target VMs demo remediation on a **real host**: they ship a vulnerable OpenSSH
  (`1:8.9p1-3`) that the agent upgrades to the patched build. The vulnerable version is
  pulled from Ubuntu's frozen `jammy/main` pocket, and `unattended-upgrades` is disabled
  on boot so it stays vulnerable until the agent fixes it. To use a different host vuln,
  edit `openssh_vulnerable_version` or `templates/target-vm.sh.tftpl`.
