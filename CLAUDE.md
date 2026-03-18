# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Face recognition system with three services: Java Spring Boot backend (auth, RBAC, face orchestration, audit), Python Flask AI service (face detection/recognition with RetinaFace/ArcFace), and React TypeScript SPA frontend. MySQL for persistence, Redis for token blacklist, MinIO for face images, RabbitMQ for audit event pipeline, Elasticsearch for audit search.

See `backend-service/CLAUDE.md` and `gui-app/CLAUDE.md` for detailed service-specific guidance.

## Common Commands

### Full Stack (Docker Compose)
```bash
docker-compose up -d                  # Start all services
docker-compose logs -f backend-service  # Tail backend logs
curl http://localhost:8080/ping       # Health check → {"data":"pong"}
```

### Backend (Java 17 + Spring Boot 3.3.4)
```bash
cd backend-service
./gradlew bootRun                     # Dev server (port 8080)
./gradlew test                        # Unit + integration tests (Testcontainers)
./gradlew e2eTest                     # E2E tests (requires docker-compose stack running)
./gradlew jacocoTestReport            # Coverage report (HTML + XML)
./gradlew jacocoTestCoverageVerification  # Enforce coverage thresholds
./gradlew compileJava                 # Compile check only
```

Run a single test class:
```bash
./gradlew test --tests "com.mario.backend.auth.service.AuthServiceTest"
```

Coverage: JaCoCo enforces 30% global minimum, 50% for `auth.service` and `auth.security` packages. DTOs, entities, and config classes are excluded.

### Frontend (React 18 + TypeScript + Vite)
```bash
cd gui-app
pnpm install                          # Install dependencies
pnpm dev                              # Dev server (port 5173)
pnpm build                            # Production build (tsc -b && vite build)
pnpm lint                             # ESLint
npx tsc --noEmit                      # Type check only
```

### AI Service (Python 3.10+ / Flask)
```bash
cd face-ai-service
pip install -r requirements.txt
python app.py                         # Dev server (port 5000)
```

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

Deployment paths:
1. **Local dev:** `docker-compose up -d`
2. **VM (Ansible):** `ci-scripts/make deploy-{dev,staging,prod}` — authoritative path for all environments

CI runs per-service workflows (`.github/workflows/`) using reusable templates from the `ci-scripts/baseline/` submodule.
