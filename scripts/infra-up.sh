#!/usr/bin/env bash
# Bring up the shared infra (mysql, redis, minio, rabbitmq, elasticsearch)
# for face-microservice on this machine. Run once per machine; idempotent.
#
# Usage:
#   ./scripts/infra-up.sh            # bring up and wait for health
#   ./scripts/infra-up.sh --no-wait  # don't block on health checks
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

COMPOSE_FILE="docker-compose.infra.yml"
WAIT=1
[[ "${1:-}" == "--no-wait" ]] && WAIT=0

echo ">> Pulling infra images..."
docker compose -f "$COMPOSE_FILE" pull

echo ">> Starting infra..."
docker compose -f "$COMPOSE_FILE" up -d

if [[ "$WAIT" -eq 1 ]]; then
  echo ">> Waiting for services to report healthy (2 min timeout)..."
  for svc in mysql redis minio rabbitmq elasticsearch; do
    printf "   %-15s " "$svc"
    for i in $(seq 1 60); do
      state=$(docker inspect --format '{{.State.Health.Status}}' "$svc" 2>/dev/null || echo "missing")
      if [[ "$state" == "healthy" ]]; then
        echo "healthy"
        break
      fi
      if [[ "$i" -eq 60 ]]; then
        echo "TIMEOUT (state=$state)"
        exit 1
      fi
      sleep 2
    done
  done
fi

echo
echo ">> Infra status:"
docker compose -f "$COMPOSE_FILE" ps
echo
echo ">> Network 'face-microservice' is ready. App containers (deployed by CI)"
echo "   will join this network as external."
