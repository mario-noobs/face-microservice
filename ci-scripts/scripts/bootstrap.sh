#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Delegate to baseline's bootstrap script
"$SCRIPT_DIR/../baseline/scripts/bootstrap.sh"
