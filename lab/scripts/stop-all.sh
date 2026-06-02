#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAB_DIR="$(dirname "$SCRIPT_DIR")"

source "$SCRIPT_DIR/deploy-common.sh"

check_docker

cd "$LAB_DIR"

log_info "Stopping all lab containers..."
docker compose down

log_success "Lab stopped successfully"
