# Face Recognition System

A production-ready face recognition system with authentication, RBAC, audit logging, and AI-powered face detection/recognition.

## Architecture

```
┌─────────────────┐     ┌─────────────────────┐     ┌────────────────────┐
│   Frontend      │────▶│   Backend Service   │────▶│  Face Recognition  │
│   (React)       │     │   (Java/Spring)     │     │     (Python)       │
│   Port: 80      │     │   Port: 8080        │     │   Port: 5000       │
└─────────────────┘     └─────────────────────┘     └────────────────────┘
                               │
                    ┌──────────┼──────────┬──────────┐
                    ▼          ▼          ▼          ▼
              ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌──────────┐
              │  MySQL  │ │  Redis  │ │  MinIO  │ │ RabbitMQ │
              │  :3306  │ │  :6379  │ │  :9000  │ │  :5672   │
              └─────────┘ └─────────┘ └─────────┘ └──────────┘
```

## Quick Start — Docker Compose (Local Dev)

```bash
# Start all services
docker-compose up -d

# Verify backend is running
curl http://localhost:8080/ping
# {"data":"pong"}

# View logs
docker-compose logs -f backend-service

# Start with logging stack (optional)
docker-compose --profile logging up -d
```

## Services

| Service | Technology | Port | Description |
|---------|------------|------|-------------|
| **backend-service** | Java Spring Boot 3.3 | 8080 | REST API, Auth, RBAC, Face orchestration, Audit |
| **face-recognition-service** | Python Flask + PyTorch | 5000 | AI Face Detection & Recognition |
| **gui-app** | React 18 + TypeScript | 80 | Web Frontend (nginx) |
| **mysql** | MySQL 8.0 | 3306 | Primary Database |
| **redis** | Redis 7.4 | 6379 | Token Blacklist & Cache |
| **minio** | MinIO | 9000/9001 | Object Storage (S3-compatible) |
| **rabbitmq** | RabbitMQ 3.13 | 5672/15672 | Audit Event Pipeline |

## API Endpoints

### Authentication (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/user/authenticate` | Login with email/password |
| POST | `/api/v1/user/register` | Register new user |
| POST | `/api/v1/user/logout` | Logout (blacklist token) |
| POST | `/api/v1/user/refresh` | Refresh access token |
| POST | `/api/v1/user/forgot-password` | Request password reset email |
| POST | `/api/v1/user/reset-password` | Reset password with token |
| POST | `/api/v1/user/accept-invitation` | Accept invitation & set password |

### Protected (JWT Required)

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/v1/profile` | `user:read_self` | Get profile |
| PUT | `/api/v1/profile` | `user:update_self` | Update profile |
| PUT | `/api/v1/user/change-password` | `user:update_self` | Change password |
| POST | `/api/v1/face/register-identity` | `face:register` | Register face |
| POST | `/api/v1/face/recognize-identity` | `face:recognize` | Recognize face |
| POST | `/api/v1/face/delete-identity` | `face:delete` | Delete face data |
| GET | `/api/v1/face/is-registered` | `face:check` | Check registration |
| GET | `/api/v1/audit/all` | `audit:read_all` | All audit logs |
| GET | `/api/v1/audit/user/{id}` | `audit:read_all` or own | User audit logs |

### Admin (SUPERADMIN)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/admin/users` | List users (paginated) |
| GET | `/api/v1/admin/users/{id}` | Get user |
| PUT | `/api/v1/admin/users/{id}` | Update user |
| PUT | `/api/v1/admin/users/{id}/status` | Activate/deactivate/ban |
| POST | `/api/v1/admin/users/invite` | Invite user via email |
| GET | `/api/v1/admin/rbac/roles` | List roles |
| POST | `/api/v1/admin/rbac/roles` | Create role |
| PUT | `/api/v1/admin/rbac/roles/{id}` | Update role |
| DELETE | `/api/v1/admin/rbac/roles/{id}` | Delete role |
| GET | `/api/v1/admin/rbac/permissions` | List permissions |
| PUT | `/api/v1/admin/rbac/roles/{id}/permissions` | Set role permissions |
| PUT | `/api/v1/admin/rbac/users/{id}/role` | Assign role to user |

## Project Structure

```
face-microservice/
├── backend-service/          # Java Spring Boot modular monolith
│   └── src/main/java/com/mario/backend/
│       ├── auth/             # Authentication, JWT, password reset, invitation
│       ├── users/            # User profiles, admin user management
│       ├── rbac/             # Role & permission management
│       ├── face/             # Face recognition (calls Python AI service)
│       ├── audit/            # Audit logging pipeline (RabbitMQ → MySQL/ES)
│       ├── gateway/          # Security config, filters, infrastructure
│       ├── common/           # Shared DTOs, exceptions
│       └── logging/          # Distributed tracing, log masking
├── face-regconition-service/ # Python AI service (RetinaFace + ArcFace)
├── gui-app/                  # React 18 + TypeScript SPA
│   └── src/modules/
│       ├── core/             # Router, Layout, Axios interceptor
│       ├── auth/             # Login, Register, ForgotPassword, Invitation
│       ├── home/             # Dashboard, Profile, Audit
│       ├── face-reg/         # Face register & recognize UI
│       └── admin/            # User & Role management (SUPERADMIN)
├── email-service/            # Email library (FreeMarker templates)
├── ci-scripts/               # CI/CD configuration
│   ├── baseline/             # Submodule → ci-baseline (shared infra)
│   └── ansible/              # Ansible overlay (VM deploy)
├── logging/                  # Fluent-bit configs
├── docker-compose.yml
├── nginx.conf
└── init.sql                  # Database seed (roles, permissions)
```

## Development

```bash
# Backend (Java 17+)
cd backend-service
./gradlew bootRun

# Frontend (Node 18+)
cd gui-app
pnpm install && pnpm dev

# AI Service (Python 3.10+)
cd face-regconition-service
pip install -r requirements.txt
python app.py
```

## Key Features

- **JWT Authentication** with access/refresh token rotation and Redis-backed blacklist
- **RBAC** with database-backed roles and permissions embedded in JWT claims
- **Forgot Password** flow with email token reset
- **Admin User Invitation** with email-based onboarding and auto-login
- **User Status Management** with immediate token invalidation on ban/deactivate
- **Face Recognition** powered by RetinaFace (detection) + ArcFace (recognition)
- **Audit Pipeline** via RabbitMQ to MySQL and Elasticsearch
- **Distributed Tracing** with correlation IDs across services

## Environment Variables

```bash
# Backend Service
DB_HOST=mysql                 DB_PORT=3306
DB_NAME=backend_db            DB_USERNAME=root        DB_PASSWORD=<password>
REDIS_HOST=redis              REDIS_PORT=6379
RABBITMQ_HOST=rabbitmq        RABBITMQ_PORT=5672
JWT_SECRET=<256-bit-secret>
MINIO_ENDPOINT=http://minio:9000
FACE_SERVICE_URL=http://face-recognition-service:5000
FRONTEND_URL=http://localhost
MAIL_HOST=smtp.gmail.com      MAIL_USERNAME=...       MAIL_PASSWORD=...
```

## Deployment

| Method | Technology | Use Case |
|--------|-----------|----------|
| **Docker Compose** | docker-compose up | Local development |
| **Ansible + SSH** | `make deploy-dev` via ci-scripts | VM-based environments |

## Documentation

- **[CLAUDE.md](./CLAUDE.md)** — Full technical documentation (architecture, security, RBAC, scaling)
- **[ci-scripts/README.md](./ci-scripts/README.md)** — CI/CD configuration (Ansible deploys)
- **[backend-service/README.md](./backend-service/README.md)** — Backend build, API reference, testing
- **[gui-app/README.md](./gui-app/README.md)** — Frontend architecture, routing, auth flow

## Troubleshooting

```bash
# Service health
curl http://localhost:8080/ping

# View logs
docker-compose logs -f backend-service

# Database
docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"

# Redis
docker-compose exec redis redis-cli ping

# MinIO
curl http://localhost:9000/minio/health/live
```

## License

MIT License - see [LICENSE](LICENSE) for details.
