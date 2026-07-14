#!/usr/bin/env bash
# Post-deploy verification for the AWS vuln lab. Reads `terraform output`, then checks:
#   - docker-lab host: bootstrap done, all lab containers up, services exploitable, ports reachable from here
#   - target VMs:      bootstrap done, sshd active, OpenSSH in the VULNERABLE state (ready to demo)
# Nothing is changed. Exit code is non-zero if any check fails.
#
# Usage:   ./scripts/verify-lab.sh
# SSH key: export SSH_KEY=~/.ssh/your_key   (optional; falls back to your default agent/key)
set -uo pipefail

SSH_USER="${SSH_USER:-ubuntu}"
VULN_OPENSSH="1:8.9p1-3"
SSH_OPTS=(-o StrictHostKeyChecking=accept-new -o ConnectTimeout=10 -o BatchMode=yes)
[ -n "${SSH_KEY:-}" ] && SSH_OPTS+=(-i "$SSH_KEY")

cd "$(dirname "$0")/.." || exit 1

if command -v tput >/dev/null 2>&1 && [ -t 1 ]; then
  G=$(tput setaf 2); R=$(tput setaf 1); Y=$(tput setaf 3); B=$(tput bold); N=$(tput sgr0)
else G=; R=; Y=; B=; N=; fi

PASS=0; FAIL=0
ok()   { echo "  ${G}PASS${N} $1"; PASS=$((PASS+1)); }
bad()  { echo "  ${R}FAIL${N} $1"; FAIL=$((FAIL+1)); }
warn() { echo "  ${Y}WARN${N} $1"; }
sshc() { ssh "${SSH_OPTS[@]}" "$SSH_USER@$1" "$2" 2>/dev/null; }

tfout()     { terraform output -raw "$1" 2>/dev/null; }
tfout_json(){ terraform output -json "$1" 2>/dev/null; }

LAB_IP=$(tfout docker_lab_public_ip)
if [ -z "$LAB_IP" ]; then
  echo "${R}No terraform outputs found. Run 'terraform apply' first (and from the module dir).${N}"
  exit 2
fi
mapfile -t VM_IPS < <(tfout_json target_vm_public_ips | python3 -c 'import sys,json;[print(x) for x in json.load(sys.stdin)]' 2>/dev/null)

echo "${B}== Docker-lab host ($LAB_IP) ==${N}"
if sshc "$LAB_IP" 'true'; then
  ok "SSH reachable"
  sshc "$LAB_IP" 'grep -q "bootstrap done" /var/log/lab-bootstrap.log' \
    && ok "cloud-init bootstrap completed" \
    || warn "bootstrap not finished yet — tail: ssh $SSH_USER@$LAB_IP 'sudo tail -f /var/log/lab-bootstrap.log'"
  UP=$(sshc "$LAB_IP" 'sudo docker compose -f /root/lab/docker-compose.yml ps --status running -q 2>/dev/null | wc -l')
  [ "${UP:-0}" -ge 10 ] && ok "lab containers running ($UP)" || bad "only ${UP:-0} lab containers up (expected ~11) — 'sudo docker compose -f /root/lab/docker-compose.yml ps'"
  # exploitability probes (run on the host, against localhost)
  sshc "$LAB_IP" 'curl -s http://localhost:2375/version'   | grep -q ApiVersion && ok "docker-api 2375 exposed (unauth)" || bad "docker-api 2375 not responding"
  sshc "$LAB_IP" 'curl -s "http://localhost:8000/?name={{7*7}}"' | grep -q 49  && ok "flask SSTI live (7*7=49)"        || bad "flask SSTI probe failed"
  for pp in "drupal:8081" "tomcat:8082" "thinkphp:8083" "nodejs:3000"; do
    n=${pp%:*}; p=${pp#*:}
    sshc "$LAB_IP" "curl -s -o /dev/null -w '%{http_code}' http://localhost:$p" | grep -qE '^(200|30.|40.)$' \
      && ok "$n reachable on :$p" || bad "$n not responding on :$p"
  done
else
  bad "SSH to docker-lab host failed (check SSH_KEY, and that admin_cidr covers your current IP: curl -s ifconfig.me)"
fi

echo "${B}== External reachability (from this machine → security-group check) ==${N}"
for pp in "drupal:8081" "docker-api:2375"; do
  n=${pp%:*}; p=${pp#*:}
  curl -s -o /dev/null -m 8 -w '%{http_code}' "http://$LAB_IP:$p" 2>/dev/null | grep -qE '^[0-9]{3}$' \
    && ok "reachable from here: $n ($LAB_IP:$p)" \
    || bad "NOT reachable from here: $n ($LAB_IP:$p) — likely admin_cidr/SG. Your IP: $(curl -s ifconfig.me 2>/dev/null)"
done

echo "${B}== Target VMs (real remediation targets) ==${N}"
if [ "${#VM_IPS[@]}" -eq 0 ]; then warn "no target VM IPs in outputs"; fi
for ip in "${VM_IPS[@]}"; do
  [ -z "$ip" ] && continue
  echo "  ${B}$ip${N}"
  if sshc "$ip" 'true'; then
    sshc "$ip" 'grep -q "bootstrap done" /var/log/target-bootstrap.log' && ok "bootstrap completed" || warn "bootstrap not finished yet"
    sshc "$ip" 'systemctl is-active --quiet ssh' && ok "sshd active" || bad "sshd not active"
    VER=$(sshc "$ip" "dpkg-query -W -f='\${Version}' openssh-server")
    if [ "$VER" = "$VULN_OPENSSH" ]; then ok "OpenSSH $VER — ${B}VULNERABLE, ready to demo${N}"
    elif [ -n "$VER" ];              then warn "OpenSSH $VER — already patched/remediated. Re-arm: ./scripts/reset-target.sh $ip"
    else bad "could not read openssh-server version"; fi
    sshc "$ip" 'systemctl is-enabled --quiet unattended-upgrades 2>/dev/null' \
      && warn "unattended-upgrades still enabled (may auto-patch before demo)" \
      || ok "auto-patching disabled (stays vulnerable)"
  else
    bad "SSH failed to $ip"
  fi
done

echo
echo "${B}== Summary: ${G}$PASS passed${N}${B}, ${R}$FAIL failed${N}${B} =="${N}
[ "$FAIL" -eq 0 ]
