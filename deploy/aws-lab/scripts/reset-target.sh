#!/usr/bin/env bash
# Re-arm target VM(s): downgrade OpenSSH back to the vulnerable version so the
# remediation demo can be run again — WITHOUT recreating the instance. Use this after a
# rehearsal where the agent already remediated (upgraded) the target.
#
# Usage:
#   ./scripts/reset-target.sh                # all target VMs from terraform output
#   ./scripts/reset-target.sh 1.2.3.4 5.6.7.8
# SSH key: export SSH_KEY=~/.ssh/your_key   (optional)
set -uo pipefail

SSH_USER="${SSH_USER:-ubuntu}"
VULN_OPENSSH="${VULN_OPENSSH:-1:8.9p1-3}"
SSH_OPTS=(-o StrictHostKeyChecking=accept-new -o ConnectTimeout=10 -o BatchMode=yes)
[ -n "${SSH_KEY:-}" ] && SSH_OPTS+=(-i "$SSH_KEY")

cd "$(dirname "$0")/.." || exit 1

IPS=("$@")
if [ "${#IPS[@]}" -eq 0 ]; then
  while IFS= read -r _ip; do [ -n "$_ip" ] && IPS+=("$_ip"); done \
    < <(terraform output -json target_vm_public_ips 2>/dev/null \
      | python3 -c 'import sys,json;[print(x) for x in json.load(sys.stdin)]' 2>/dev/null)
fi
if [ "${#IPS[@]}" -eq 0 ]; then
  echo "No target IPs (pass them as args, or run 'terraform apply' first)." >&2
  exit 2
fi

REMOTE="set -e
export DEBIAN_FRONTEND=noninteractive
sudo systemctl disable --now unattended-upgrades apt-daily.timer apt-daily-upgrade.timer 2>/dev/null || true
sudo apt-get update -y
sudo apt-get install -y --allow-downgrades \
  openssh-server=$VULN_OPENSSH openssh-client=$VULN_OPENSSH openssh-sftp-server=$VULN_OPENSSH
sudo systemctl restart ssh
echo -n 'now: '; dpkg-query -W -f='\${Version}\n' openssh-server"

rc=0
for ip in "${IPS[@]}"; do
  [ -z "$ip" ] && continue
  echo "== re-arming $ip =="
  if ssh "${SSH_OPTS[@]}" "$SSH_USER@$ip" "$REMOTE"; then
    echo "   OK: $ip is vulnerable again"
  else
    echo "   FAILED: $ip" >&2; rc=1
  fi
done
exit $rc
