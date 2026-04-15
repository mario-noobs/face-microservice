#!/usr/bin/env bash
# Roll a service back to the tag it had in HEAD~1 of versions.yaml for a given env.
# Commits the rollback as a manifest entry (rollback IS a deploy event) and
# triggers an Ansible deploy of the service.
#
# Usage:
#   SERVICE=backend-service ./deploy-previous.sh            # defaults to ENV=dev
#   SERVICE=face-ai-service ENV=staging ./deploy-previous.sh
#
# Requires: yq, ansible-playbook, git, and the vault password file at
# /tmp/.vault_pass (same convention as the other deploy scripts).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

ENV="${ENV:-dev}"
: "${SERVICE:?Usage: SERVICE=<name> [ENV=dev] $0}"

case "$SERVICE" in
  backend-service|face-ai-service|gui-app) ;;
  *) echo "ERROR: unknown service '$SERVICE'" >&2; exit 1 ;;
esac

case "$ENV" in
  dev|staging|production) ;;
  *) echo "ERROR: unknown env '$ENV'" >&2; exit 1 ;;
esac

cd "$REPO_ROOT"

if ! command -v yq >/dev/null 2>&1; then
  echo "ERROR: yq not installed. brew install yq (or equivalent)" >&2
  exit 1
fi

CUR_TAG=$(yq ".environments.${ENV}.\"${SERVICE}\"" versions.yaml)
PREV_TAG=$(git show HEAD~1:versions.yaml 2>/dev/null | yq ".environments.${ENV}.\"${SERVICE}\"" || echo "null")

if [ -z "$PREV_TAG" ] || [ "$PREV_TAG" = "null" ]; then
  echo "ERROR: no previous tag found for ${SERVICE} on ${ENV} in HEAD~1:versions.yaml" >&2
  exit 1
fi

if [ "$PREV_TAG" = "$CUR_TAG" ]; then
  echo "Previous tag (${PREV_TAG}) matches current (${CUR_TAG}). Nothing to do."
  exit 0
fi

echo ">> Rolling back ${SERVICE} on ${ENV}: ${CUR_TAG} -> ${PREV_TAG}"

yq eval -i ".environments.${ENV}.\"${SERVICE}\" = \"${PREV_TAG}\"" versions.yaml
git add versions.yaml
git commit -m "rollback(${ENV}): ${SERVICE} ${CUR_TAG} -> ${PREV_TAG}"

cd "$REPO_ROOT/ci-scripts/ansible"
ansible-playbook \
  -i "inventories/${ENV}/${ENV}.yml" \
  "../baseline/ansible/playbooks/deploy/deploy-service.yml" \
  -e "target_service=${SERVICE}" \
  -e "image_tag=${PREV_TAG}"

echo ">> Rollback complete. Remember to: git push origin $(git rev-parse --abbrev-ref HEAD)"
