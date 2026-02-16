#!/usr/bin/env bash
set -euo pipefail

if [ $# -eq 0 ]; then
  echo "Usage: $0 <vault-file>"
  echo "Example: $0 ansible/inventories/dev/group_vars/vault.yml"
  exit 1
fi

ansible-vault decrypt "$1"
echo "Decrypted: $1"
