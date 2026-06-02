#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(dirname "$SCRIPT_DIR")"

source "$SCRIPT_DIR/deploy-common.sh"

print_banner

check_docker
check_internet

log_info "Checking port availability..."
for port_desc in "8081:Drupal" "8082:Tomcat" "8000:Flask" "8083:ThinkPHP" "2375:Docker API"; do
    port="${port_desc%%:*}"
    name="${port_desc#*:}"
    check_port "$port" "$name" || true
done

cd "$LAB_DIR"

log_info "Cloning VulHub repository (if not present)..."
if [ ! -d "vulhub" ]; then
    git clone --depth 1 https://github.com/vulhub/vulhub.git
    log_success "VulHub cloned"
else
    log_success "VulHub already present"
fi

log_info "Copying target environments..."
mkdir -p targets

targets=(
    "drupal/CVE-2018-7600:drupal"
    "tomcat/CVE-2017-12615:tomcat"
    "flask/ssti:flask"
    "thinkphp/5-rce:thinkphp"
    "docker/unauthorized-rce:docker"
)

for entry in "${targets[@]}"; do
    src="${entry%%:*}"
    dst="${entry#*:}"
    if [ -d "targets/$dst" ]; then
        log_success "$dst already in targets/"
    elif [ -d "vulhub/$src" ]; then
        cp -r "vulhub/$src" "targets/$dst"
        log_success "Copied $dst from vulhub/$src"
    else
        log_error "VulHub source not found: vulhub/$src"
        exit 1
    fi
done

log_info "Building and starting containers..."
docker compose build --quiet
docker compose up -d

log_info "Waiting for containers to initialize..."
sleep 5

log_info "Checking target reachability..."
all_ok=true

check_target() {
    local name=$1
    local url=$2
    local type=${3:-http}

    if [ "$type" = "http" ]; then
        if curl -sf --connect-timeout 5 --max-time 10 "$url" &>/dev/null; then
            log_success "$name is reachable at $url"
            return 0
        else
            log_warn "$name is NOT reachable at $url"
            return 1
        fi
    else
        if curl -sf --connect-timeout 5 --max-time 10 "$url" | grep -q "ApiVersion"; then
            log_success "$name is reachable at $url"
            return 0
        else
            log_warn "$name is NOT reachable at $url"
            return 1
        fi
    fi
}

check_target "Drupal"     "http://localhost:8081" || all_ok=false
check_target "Tomcat"     "http://localhost:8082" || all_ok=false
check_target "Flask"      "http://localhost:8000" || all_ok=false
check_target "ThinkPHP"   "http://localhost:8083" || all_ok=false
check_target "Docker API" "http://localhost:2375/version" "api" || all_ok=false

echo ""
if [ "$all_ok" = true ]; then
    log_success "All 5 targets are running and reachable!"
else
    log_warn "Some targets may still be starting. Wait a moment and run: docker compose ps"
fi

echo ""
echo "  Targets available:"
echo "    Drupal (CVE-2018-7600):  http://localhost:8081"
echo "    Tomcat (CVE-2017-12615): http://localhost:8082"
echo "    Flask (SSTI):            http://localhost:8000"
echo "    ThinkPHP (CVE-2018-20062): http://localhost:8083"
echo "    Docker API (unauth):     http://localhost:2375"
echo ""
echo "  Run verification:  ./scripts/verify-all.sh"
echo "  Stop lab:          ./scripts/stop-all.sh"
echo "  Reset lab:         ./scripts/reset-all.sh"
echo ""
