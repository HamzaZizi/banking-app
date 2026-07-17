#!/usr/bin/env bash
#
# Stop and remove the C&I Banking demo containers (backend + frontend).
#
# Usage:
#   ./stop.sh              # remove the demo containers
#   ./stop.sh --engine     # also stop the Colima engine (frees all resources)
#
set -euo pipefail

BACKEND_NAME="ci-backend"
FRONTEND_NAME="ci-frontend"

log() { printf '\033[0;35m==>\033[0m %s\n' "$*"; }

if docker info >/dev/null 2>&1; then
  log "Stopping and removing demo containers..."
  docker rm -f "$BACKEND_NAME" "$FRONTEND_NAME" >/dev/null 2>&1 || true
  log "Containers removed."
else
  log "Docker daemon not running, nothing to remove."
fi

if [[ "${1:-}" == "--engine" ]]; then
  if command -v colima >/dev/null 2>&1; then
    log "Stopping Colima engine..."
    colima stop || true
  fi
fi

log "Done."
