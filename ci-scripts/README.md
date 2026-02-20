# ci-scripts (face-microservice)

Project-specific CI/CD overlay for the face-microservice project. Uses [ci-baseline](https://github.com/mario-noobs/ci-baseline) as a git submodule for reusable roles, playbooks, workflows, and K8s infrastructure components.

## Two Deployment Paths

| Path | Technology | When to Use |
|------|-----------|-------------|
| **Ansible** (existing) | Ansible + SSH + docker-compose | Deploy to VMs (dev/staging/prod servers) |
| **K8s + ArgoCD** (new) | Kustomize + ArgoCD + k3d/EKS/GKE | GitOps deploy to Kubernetes clusters |

## Structure

```
ci-scripts/
├── baseline/                    ← git submodule → mario-noobs/ci-baseline
│   ├── ansible/roles/          ← Generic roles (common, docker, security, etc.)
│   ├── ansible/playbooks/      ← Generic playbooks (deploy-all, rollback, etc.)
│   ├── .github/workflows/      ← Reusable GH Actions (_ci-java, _docker-build, _cd-update-image, etc.)
│   ├── k8s/                    ← Reusable K8s infrastructure components
│   │   ├── components/         ← Kustomize Components (mysql, redis, minio, rabbitmq, es, vault)
│   │   └── scripts/            ← Cluster setup automation (k3d, argocd, local registry)
│   └── scripts/deploy.sh       ← Universal deploy wrapper
│
├── ansible/                    ← Project Ansible overlay (VM deploys)
│   ├── ansible.cfg
│   ├── group_vars/all/commons.yml
│   ├── inventories/            ← dev, staging, production environments
│   └── files/                  ← Project-specific init.sql + nginx.conf.j2
│
├── k8s/                        ← Project K8s overlay (Kubernetes deploys)
│   ├── base/                   ← App manifests + imports baseline infra components
│   │   ├── apps/               ← backend, face-service, frontend
│   │   ├── jobs/               ← db-init, minio-init (ArgoCD hooks)
│   │   ├── ingress.yaml
│   │   └── kustomization.yaml  ← Imports components from baseline
│   ├── overlays/               ← Environment-specific configs
│   │   ├── dev/                ← Local k3d (k3d-registry images, dev secrets)
│   │   ├── staging/            ← GHCR images, medium resources
│   │   └── prod/               ← Managed services, infra scaled to 0, TLS
│   └── argocd/                 ← ArgoCD Application definitions
│       ├── project.yaml        ← AppProject
│       ├── app-of-apps.yaml    ← Root Application
│       ├── face-app-dev.yaml   ← Auto-sync + self-heal
│       ├── face-app-staging.yaml
│       └── face-app-prod.yaml  ← Manual sync for safety
│
├── scripts/                    ← Project deploy wrappers
└── Makefile
```

## Quick Start — Ansible (VMs)

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

## Quick Start — K8s + ArgoCD (Local)

```bash
# Prerequisites: docker, k3d, kubectl

# 1. Create k3d cluster with nginx-ingress
ci-scripts/baseline/k8s/scripts/cluster-create.sh

# 2. Build and push images to local registry
ci-scripts/baseline/k8s/scripts/build-push-local.sh .

# 3. Install ArgoCD
ci-scripts/baseline/k8s/scripts/argocd-install.sh

# 4. Apply ArgoCD apps (ArgoCD takes over from here)
kubectl apply -f ci-scripts/k8s/argocd/project.yaml
kubectl apply -f ci-scripts/k8s/argocd/face-app-dev.yaml

# 5. Add host entry and test
echo "127.0.0.1 face.local" | sudo tee -a /etc/hosts
curl http://face.local/ping

# Teardown
ci-scripts/baseline/k8s/scripts/cluster-delete.sh
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
| `_cd-update-image.yml` | GitOps: update image tag in Kustomize overlay |

### CI → CD Flow (GitOps)

```
Code push → CI workflow (build + test + push image to GHCR)
         → cd.yml (resolve image name)
         → _cd-update-image.yml (kustomize edit set image + git push)
         → ArgoCD detects change → syncs to cluster
```

## K8s Infrastructure Components

Reusable Kustomize Components from `baseline/k8s/components/`:

| Component | Image | Ports | Storage |
|-----------|-------|-------|---------|
| mysql | mysql:8.0 | 3306 | 5Gi PVC |
| redis | redis:7.4-alpine | 6379 | 1Gi PVC |
| minio | quay.io/minio/minio | 9000, 9001 | 5Gi PVC |
| rabbitmq | rabbitmq:3.13-management-alpine | 5672, 15672 | 2Gi PVC |
| elasticsearch | elasticsearch:8.11.0 | 9200, 9300 | 5Gi PVC |
| vault | hashicorp/vault:1.15 | 8200 | none (dev mode) |

Projects import only what they need via `components:` in their kustomization.yaml.

## Environments

### Ansible

- `ansible/inventories/dev/` — Development
- `ansible/inventories/staging/` — Staging
- `ansible/inventories/production/` — Production

### Kubernetes

- `k8s/overlays/dev/` — Local k3d (k3d-registry images)
- `k8s/overlays/staging/` — GHCR images, medium resources
- `k8s/overlays/prod/` — Managed services (RDS, ElastiCache, S3), TLS, higher replicas

## Vault

Secrets are encrypted with `ansible-vault`. Decrypt helper:

```bash
./scripts/decrypt-vault.sh ansible/inventories/dev/group_vars/vault.yml
```
