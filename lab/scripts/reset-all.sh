#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(dirname "$SCRIPT_DIR")"

source "$SCRIPT_DIR/deploy-common.sh"

check_docker

cd "$LAB_DIR"

log_info "Stopping containers and removing volumes..."
docker compose down -v --remove-orphans

log_info "Removing locally built images (Tomcat, Docker API)..."
docker compose build --quiet 2>/dev/null || true
docker compose down --rmi local 2>/dev/null || true

log_info "Removing target directories to get clean copies..."
for dir in targets/drupal targets/tomcat targets/flask targets/thinkphp targets/docker; do
    if [ -d "$dir" ]; then
        rm -rf "$dir"
        log_success "Removed $dir"
    fi
done

log_info "Re-running deploy..."
exec "$SCRIPT_DIR/deploy-all.sh"
