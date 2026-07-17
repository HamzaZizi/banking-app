#!/usr/bin/env bash
#
# Local dev launcher for the C&I Banking demo (backend + frontend containers).
# Reproduces the manual setup: start Colima, build images, run both containers.
#
# Usage:
#   ./start.sh            # build (if needed) and start
#   ./start.sh --rebuild  # force a rebuild of both images
#
set -euo pipefail

cd "$(dirname "$0")"

# --- Config ---------------------------------------------------------------
BACKEND_IMAGE="ci-banking-backend"
FRONTEND_IMAGE="ci-banking-frontend"
RATES_IMAGE="ci-banking-rates"
BACKEND_NAME="ci-backend"
FRONTEND_NAME="ci-frontend"
RATES_NAME="ci-rates"
NETWORK="ci-banking-net"
BACKEND_PORT="8080"
FRONTEND_PORT="8081"
RATES_PORT="9090"
API_BASE_URL="http://localhost:${BACKEND_PORT}"
# Backend reaches the downstream by container name over the shared docker network.
DOWNSTREAM_URL="http://${RATES_NAME}:9090"
# Apple Silicon: the backend's alpine base image has no arm64 build, so we
# build/run for amd64 under emulation. Override with PLATFORM=linux/arm64 if desired.
PLATFORM="${PLATFORM:-linux/amd64}"

REBUILD=false
[[ "${1:-}" == "--rebuild" ]] && REBUILD=true

log() { printf '\033[0;35m==>\033[0m %s\n' "$*"; }

# --- 1. Ensure a container engine is running ------------------------------
if ! command -v docker >/dev/null 2>&1; then
  echo "docker CLI not found. Install with: brew install docker colima" >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  log "Docker daemon not reachable, starting Colima..."
  if ! command -v colima >/dev/null 2>&1; then
    echo "colima not found. Install with: brew install colima" >&2
    exit 1
  fi
  colima start
fi

# --- 2. Build images (skip if present, unless --rebuild) ------------------
build_if_needed() {
  local image="$1" context="$2"
  if $REBUILD || ! docker image inspect "$image" >/dev/null 2>&1; then
    log "Building ${image} (${PLATFORM})..."
    docker build --platform "$PLATFORM" -t "$image" "$context"
  else
    log "Image ${image} already present, skipping build (use --rebuild to force)."
  fi
}

build_if_needed "$BACKEND_IMAGE" backend/
build_if_needed "$FRONTEND_IMAGE" frontend/
build_if_needed "$RATES_IMAGE" rates-service/

# --- 3. (Re)start containers ----------------------------------------------
log "Ensuring docker network ${NETWORK} exists..."
docker network create "$NETWORK" >/dev/null 2>&1 || true

log "Removing any existing containers..."
docker rm -f "$BACKEND_NAME" "$FRONTEND_NAME" "$RATES_NAME" >/dev/null 2>&1 || true

log "Starting downstream rates-service on :${RATES_PORT}..."
docker run -d --platform "$PLATFORM" --name "$RATES_NAME" --network "$NETWORK" \
  -p "${RATES_PORT}:9090" "$RATES_IMAGE" >/dev/null

log "Starting backend on :${BACKEND_PORT}..."
docker run -d --platform "$PLATFORM" --name "$BACKEND_NAME" --network "$NETWORK" \
  -p "${BACKEND_PORT}:8080" -e DOWNSTREAM_URL="$DOWNSTREAM_URL" "$BACKEND_IMAGE" >/dev/null

log "Starting frontend on :${FRONTEND_PORT}..."
docker run -d --platform "$PLATFORM" --name "$FRONTEND_NAME" --network "$NETWORK" \
  -p "${FRONTEND_PORT}:8081" -e API_BASE_URL="$API_BASE_URL" "$FRONTEND_IMAGE" >/dev/null

# --- 4. Wait for the backend to be healthy --------------------------------
log "Waiting for backend to become healthy..."
for i in $(seq 1 30); do
  if curl -sf "http://localhost:${BACKEND_PORT}/actuator/health" >/dev/null 2>&1; then
    break
  fi
  sleep 1
  [[ $i -eq 30 ]] && { echo "Backend did not become healthy in time. Check: docker logs ${BACKEND_NAME}" >&2; exit 1; }
done

echo
log "Up and running:"
echo "   Frontend : http://localhost:${FRONTEND_PORT}   <- open this"
echo "   Backend  : http://localhost:${BACKEND_PORT}/api/summary"
echo "   Rates    : http://localhost:${BACKEND_PORT}/api/rates   (integration -> downstream)"
echo "   Logs     : docker logs -f ${BACKEND_NAME}"
echo "   Stop     : ./stop.sh"
