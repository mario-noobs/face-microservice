#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Call baseline's universal deploy wrapper
"$SCRIPT_DIR/../baseline/scripts/deploy.sh" dev "$@"
