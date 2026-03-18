# ci-scripts (face-microservice)

Project-specific CI/CD overlay for the face-microservice project. Uses [ci-baseline](https://github.com/mario-noobs/ci-baseline) as a git submodule for reusable roles, playbooks, and workflows.

## Structure

```
ci-scripts/
├── baseline/                    ← git submodule → mario-noobs/ci-baseline
│   ├── ansible/roles/          ← Generic roles (common, docker, security, etc.)
│   ├── ansible/playbooks/      ← Generic playbooks (deploy-all, rollback, etc.)
│   ├── .github/workflows/      ← Reusable GH Actions (_ci-java, _docker-build, _deploy, etc.)
│   └── scripts/deploy.sh       ← Universal deploy wrapper
│
├── ansible/                    ← Project Ansible overlay (VM deploys)
│   ├── ansible.cfg
│   ├── group_vars/all/commons.yml
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

# Deploy to dev
make deploy-dev
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

## Environments

- `ansible/inventories/dev/` — Development
- `ansible/inventories/staging/` — Staging
- `ansible/inventories/production/` — Production

## Vault

Secrets are encrypted with `ansible-vault`. Decrypt helper:

```bash
./scripts/decrypt-vault.sh ansible/inventories/dev/group_vars/vault.yml
```
