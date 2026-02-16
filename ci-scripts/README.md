# ci-scripts (face-microservice)

Project-specific CI/CD overlay for the face-microservice project. Uses [ci-baseline](https://github.com/mario-noobs/ci-baseline) as a git submodule for reusable roles, playbooks, and workflows.

## Structure

```
ci-scripts/
├── baseline/                    ← git submodule → mario-noobs/ci-baseline
│   ├── ansible/roles/          ← Generic roles (common, docker, security, etc.)
│   ├── ansible/playbooks/      ← Generic playbooks (deploy-all, rollback, etc.)
│   ├── .github/workflows/      ← Reusable GH Actions (_ci-java, _docker-build, etc.)
│   └── scripts/deploy.sh       ← Universal deploy wrapper
│
├── ansible/
│   ├── ansible.cfg             ← roles_path = ../baseline/ansible/roles
│   ├── group_vars/all/commons.yml  ← Project identity + file path overrides
│   ├── inventories/            ← dev, staging, production environments
│   └── files/                  ← Project-specific init.sql + nginx.conf.j2
│
├── scripts/                    ← Project deploy wrappers
└── Makefile
```

## Quick Start

```bash
# Clone with submodules
git clone --recurse-submodules <repo-url>

# Or init submodules after clone
git submodule update --init --recursive

# Install dependencies
make setup

# Lint Ansible
make lint

# Deploy to dev
make deploy-dev

# Dry-run deploy
make check
```

## Reusable Workflows

Service repos call these via `workflow_call` from baseline:

| Workflow | Purpose |
|----------|---------|
| `_ci-java.yml` | Java build, test, JaCoCo, SonarCloud |
| `_ci-python.yml` | Python lint (ruff/flake8), test |
| `_ci-node.yml` | Node build, lint, test |
| `_docker-build.yml` | Docker build + push to GHCR |
| `_security-scan.yml` | Trivy + OWASP scanning |
| `_deploy.yml` | Ansible deploy to target env |

### Usage in service repo

```yaml
jobs:
  ci:
    uses: mario-noobs/ci-baseline/.github/workflows/_ci-java.yml@main
    with:
      sonar_project_key: mario-noobs_backend-service
    secrets:
      SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

  deploy:
    needs: [ci]
    uses: mario-noobs/ci-baseline/.github/workflows/_deploy.yml@main
    with:
      environment: dev
      image_tag: sha-abc1234
      ci_scripts_path: ci-scripts
    secrets:
      SSH_PRIVATE_KEY: ${{ secrets.SSH_PRIVATE_KEY }}
      ANSIBLE_VAULT_PASSWORD: ${{ secrets.ANSIBLE_VAULT_PASSWORD }}
```

## Environments

- `ansible/inventories/dev/` — Development
- `ansible/inventories/staging/` — Staging
- `ansible/inventories/production/` — Production

## Vault

Secrets are encrypted with `ansible-vault`. Decrypt helper:

```bash
./scripts/decrypt-vault.sh ansible/inventories/dev/group_vars/vault.yml
```
