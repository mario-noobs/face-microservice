#!/usr/bin/env bash
# Quick peek at infra health + app containers on the shared network.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

echo ">> Infra containers:"
docker compose -f docker-compose.infra.yml ps

echo
echo ">> Everything on the face-microservice network:"
docker network inspect face-microservice \
  --format '{{range $k, $v := .Containers}}{{printf "  %-25s %s\n" $v.Name $v.IPv4Address}}{{end}}' \
  2>/dev/null || echo "  (network not found — run ./scripts/infra-up.sh)"
