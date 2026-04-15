#!/usr/bin/env bash
# Tear down the shared infra.
#
# Usage:
#   ./scripts/infra-down.sh          # stop and remove containers; keep volumes
#   ./scripts/infra-down.sh --wipe   # also delete volumes (DESTRUCTIVE)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

COMPOSE_FILE="docker-compose.infra.yml"

if [[ "${1:-}" == "--wipe" ]]; then
  echo ">> Stopping infra and DELETING all volumes (MySQL, MinIO data will be lost)..."
  docker compose -f "$COMPOSE_FILE" down --volumes --remove-orphans
else
  echo ">> Stopping infra (volumes preserved)..."
  docker compose -f "$COMPOSE_FILE" down --remove-orphans
fi
