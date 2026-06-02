#!/bin/bash
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/deploy-common.sh"

echo ""
echo "=========================================="
echo "  Vulnerability Verification Report"
echo "=========================================="
echo ""

total=0
passed=0
failed=0

run_check() {
    local name=$1
    local script=$2
    total=$((total + 1))
    echo -n "  $name ... "
    if bash "$script" > /dev/null 2>&1; then
        echo -e "${GREEN}PASS${NC}"
        passed=$((passed + 1))
    else
        echo -e "${RED}FAIL${NC}"
        failed=$((failed + 1))
    fi
}

run_check "Drupal (CVE-2018-7600)"   "$SCRIPT_DIR/verify-drupal.sh"
run_check "Tomcat (CVE-2017-12615)"  "$SCRIPT_DIR/verify-tomcat.sh"
run_check "Flask (SSTI)"             "$SCRIPT_DIR/verify-flask.sh"
run_check "ThinkPHP (CVE-2018-20062)" "$SCRIPT_DIR/verify-thinkphp.sh"
run_check "Docker API (unauth)"      "$SCRIPT_DIR/verify-docker-api.sh"

echo ""
echo "=========================================="
echo "  Results: $passed/$total passed, $failed failed"
echo "=========================================="
echo ""

if [ "$failed" -eq 0 ]; then
    log_success "All targets verified exploitable"
    exit 0
else
    log_warn "$failed target(s) failed verification"
    exit 1
fi
