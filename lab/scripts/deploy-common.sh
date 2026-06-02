#!/bin/bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info() { echo -e "${CYAN}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_docker() {
    if ! command -v docker &>/dev/null; then
        log_error "Docker is not installed. Please install Docker Desktop or Docker Engine."
        exit 1
    fi
    if ! docker info &>/dev/null; then
        log_error "Docker daemon is not running. Please start Docker."
        exit 1
    fi
    log_success "Docker is available and running"
}

check_port() {
    local port=$1
    local name=$2
    if lsof -i :"$port" &>/dev/null; then
        log_warn "Port $port ($name) is already in use"
        return 1
    fi
    return 0
}

check_internet() {
    if ! curl -sf --connect-timeout 5 https://github.com &>/dev/null; then
        log_error "Cannot reach github.com. Internet connection required for VulHub clone."
        exit 1
    fi
    log_success "Internet connectivity confirmed"
}

get_script_dir() {
    cd "$(dirname "${BASH_SOURCE[0]}")" && pwd
}

print_banner() {
    echo ""
    echo -e "${RED}============================================================${NC}"
    echo -e "${RED}  VULNERABLE TEST LAB — FOR AUTHORIZED TESTING ONLY${NC}"
    echo -e "${RED}============================================================${NC}"
    echo ""
    echo "  This environment contains intentionally vulnerable software."
    echo "  DO NOT expose these services to the internet or production networks."
    echo "  The Docker API target (port 2375) can compromise the host machine."
    echo ""
    echo "  Use only for local security testing and agent validation."
    echo ""
    echo -e "${RED}============================================================${NC}"
    echo ""
}
