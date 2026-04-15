# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**This is an orchestrator repo.** It pins which image tag of each service runs in each environment (`versions.yaml`), holds shared infra config (compose, nginx, init.sql, Ansible inventories), and coordinates deploys. The three deployable services live in their own GitHub repos:

- `mario-noobs/backend-service` — Java Spring Boot (auth, RBAC, face orchestration, audit)
- `mario-noobs/face-ai-service` — Python Flask AI (RetinaFace/ArcFace)
- `mario-noobs/gui-app` — React TypeScript SPA
- `mario-noobs/email-service` — library JAR consumed by backend

Infra: MySQL (persistence), Redis (token blacklist), MinIO (face images), RabbitMQ (audit event pipeline), Elasticsearch (audit search).

**Deploy model** (see `adr/ADR-005-orchestrator-versions-manifest.md`): each service CI publishes an image → fires `repository_dispatch` to this repo → `promote-dev.yml` updates `versions.yaml` and deploys. Git log of `versions.yaml` is the deploy timeline. Rollback: `make deploy-previous SERVICE=<name>` or the `rollback.yml` workflow.

Service repos have their own `CLAUDE.md`. Clone them locally if you need to iterate on source.

## Common Commands

### Full Stack (Docker Compose)
```bash
docker-compose up -d                  # Pulls images from GHCR (or builds if override.yml exists)
docker-compose logs -f backend-service
curl http://localhost:8080/ping       # {"data":"pong"}
```

For local source iteration, clone the relevant service into this directory (or as a sibling) and copy `docker-compose.override.yml.example` to `docker-compose.override.yml` — it adds `build:` directives that override the `image:` pulls. The override file is gitignored.

### Backend / Frontend / AI service
Work inside the respective service repos (`mario-noobs/backend-service`, `mario-noobs/gui-app`, `mario-noobs/face-ai-service`). Each has its own CLAUDE.md with build/test commands. Their CI publishes images to GHCR on push; the orchestrator promotes to dev automatically.

### CI/CD Scripts
```bash
cd ci-scripts
make setup                            # Install ansible, pre-commit
make lint                             # ansible-lint
make deploy-dev                       # Ansible deploy to dev
```

## Architecture

```
Browser → nginx (port 80, static + /api proxy) → Backend (8080)
                                                    ├→ MySQL (JPA, Liquibase migrations)
                                                    ├→ Redis (token blacklist)
                                                    ├→ MinIO (face images)
                                                    ├→ RabbitMQ → audit consumers (MySQL + ES + email alerts)
                                                    └→ Python AI Service (5000) → MinIO
```

### Backend: Modular Monolith (package-by-feature)
```
com.mario.backend/
├── auth/       # JWT auth, login, register, password reset, invitation
├── users/      # Profile, admin user management
├── rbac/       # Roles & permissions CRUD (SUPERADMIN-only)
├── face/       # Face operations (orchestrates calls to Python service)
├── audit/      # Audit pipeline: RabbitMQ → MySQL + Elasticsearch + alerts
├── gateway/    # SecurityConfig, filters, infra configs (MinIO, Redis, etc.)
├── common/     # ApiResponse, ErrorCode, ApiException, GlobalExceptionHandler
└── logging/    # @Traceable AOP, TraceContext MDC
```

**Module rule:** Modules communicate through service interfaces only — never import another module's repository directly.

### Frontend: Modular by Feature
```
src/modules/
├── core/       # Router, layout, shared Axios interceptor
├── auth/       # Login, register, forgot/reset password, AuthContext with RBAC helpers
├── home/       # Dashboard, profile, audit
├── face-reg/   # Face register & recognize UI
├── admin/      # User/role management (SUPERADMIN only)
└── todo/       # Todo list
```

## Key Patterns

- **Auth flow:** JWT access token (1h, contains role+permissions claims) + refresh token (7d, no role). Role changes take effect on next token refresh (reloads from DB).
- **Authorization:** `@PreAuthorize("hasAuthority('face:register')")` on controller methods. RBAC admin endpoints use `@PreAuthorize("hasRole('SUPERADMIN')")`.
- **API responses:** Uniform `ApiResponse<T>` → `{ data: T }` or `{ error: { code, message } }`.
- **Database migrations:** Liquibase (JPA ddl-auto: none). Changelogs at `backend-service/db/migration/mysql/{dev,staging,prod}/masterChangeLog.xml`, shared SQL in `common/`.
- **Audit pipeline:** HTTP request → `AuditLoggingFilter` → `AuditEventPublisher` (RabbitMQ) → consumers write to MySQL, Elasticsearch, and email alerts.
- **Frontend token refresh:** Axios interceptor catches 401 → queues concurrent requests → refreshes token → retries all queued requests.

## Deployment

- **Local dev:** `docker-compose up -d`
- **Auto-deploy to dev:** push to a service repo → service CI publishes image → fires `repository_dispatch` → this repo's `promote-dev.yml` updates `versions.yaml` and deploys via Ansible
- **Manual deploy to staging/prod:** edit `versions.yaml` for the env, commit, run `make deploy-staging` / `make deploy-prod` (or `deploy.yml` workflow_dispatch)
- **Rollback:** `make deploy-previous SERVICE=<name> [ENV=dev]` or the `rollback.yml` workflow — reads HEAD~1 of `versions.yaml` and redeploys

See `adr/ADR-005-orchestrator-versions-manifest.md` for the full model. Service CI uses reusable workflows from `ci-scripts/baseline/` (the only remaining submodule).
