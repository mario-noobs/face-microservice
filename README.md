# Face Recognition System

A production-ready face recognition system built with a modular monolith architecture.

## Architecture

```
┌─────────────────┐     ┌─────────────────────┐     ┌────────────────────┐
│   Frontend      │────▶│   Backend Service   │────▶│  Face Recognition  │
│   (React)       │     │   (Java/Spring)     │     │     (Python/AI)    │
│   Port: 80      │     │   Port: 8080        │     │   Port: 5000       │
└─────────────────┘     └─────────────────────┘     └────────────────────┘
        │                       │
        │                ┌──────┼──────┐
        │                ▼      ▼      ▼
        │          ┌─────────┐ ┌─────────┐ ┌─────────┐
        └─────────▶│  MySQL  │ │  Redis  │ │  MinIO  │
         (nginx)   │  :3306  │ │  :6379  │ │  :9000  │
                   └─────────┘ └─────────┘ └─────────┘
```

## Quick Start

```bash
# Clone the repository
git clone <repository-url>
cd face-microservice

# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps

# Check backend health
curl http://localhost:8080/ping
# Response: {"data":"pong"}

# View logs
docker-compose logs -f backend-service

# Start with logging stack (Elasticsearch, Kibana, Fluent-bit)
docker-compose --profile logging up -d
```

## Services

| Service | Technology | Port | Description |
|---------|------------|------|-------------|
| **app-fe** | React + TypeScript + Vite | 80 | Web Frontend (served via nginx) |
| **backend-service** | Java Spring Boot 3.3.4 | 8080 | Modular Monolith API |
| **face-recognition-service** | Python Flask + PyTorch | 5000 | AI Face Detection & Recognition |
| **mysql** | MySQL 8.0 | 3306 | Primary Database |
| **redis** | Redis 7.4 | 6379 | Token Blacklist & Cache |
| **minio** | MinIO | 9000, 9001 | Object Storage (S3-compatible) |

## API Endpoints

### Authentication (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/user/authenticate` | Login with email/password |
| POST | `/api/v1/user/register` | Register new user |
| POST | `/api/v1/user/logout` | Logout (blacklist token) |
| POST | `/api/v1/user/refresh` | Refresh access token |
| GET | `/ping` | Health check |

### Protected Endpoints (Require JWT)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/profile` | Get current user profile |
| PUT | `/profile` | Update user profile |
| POST | `/api/v1/face/register-identity` | Register face |
| POST | `/api/v1/face/recognize-identity` | Recognize face |
| POST | `/api/v1/face/delete-identity` | Delete face data |
| GET | `/api/v1/face/is-registered` | Check face registration status |
| GET | `/api/v1/audit/all` | Get audit logs (paginated) |

## Project Structure

```
face-microservice/
├── backend-service/              # Java Spring Boot Modular Monolith
│   ├── src/main/java/com/mario/backend/
│   │   ├── common/               # Shared utilities, DTOs, exceptions
│   │   ├── auth/                 # Authentication module
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── security/         # JWT provider, filters
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── users/                # User management module
│   │   ├── face/                 # Face recognition module
│   │   ├── audit/                # Audit logging module
│   │   └── gateway/              # Config, filters, health
│   ├── build.gradle
│   └── Dockerfile
├── face-regconition-service/     # Python AI service
├── gui-app/                      # React frontend
├── logging/                      # Fluent-bit configs
├── _archive/                     # Deprecated Go services
├── docker-compose.yml
├── nginx.conf
├── init.sql
├── CLAUDE.md                     # Detailed technical documentation
└── README.md
```

## Development

### Prerequisites
- Docker 24+ and Docker Compose 2+
- Java 17+ (for local backend development)
- Node.js 18+ and pnpm (for frontend development)
- Python 3.10+ (for AI service development)

### Local Development

```bash
# Backend (Java)
cd backend-service
./gradlew bootRun

# Frontend (React)
cd gui-app
pnpm install
pnpm dev

# AI Service (Python)
cd face-regconition-service
pip install -r requirements.txt
python app.py
```

### Build Commands

```bash
# Build backend JAR
cd backend-service
./gradlew clean build

# Build Docker images
docker-compose build

# Build specific service
docker-compose build backend-service
```

## Environment Variables

```bash
# ============ Backend Service ============
# Server
SERVER_PORT=8080

# Database
DB_HOST=mysql
DB_PORT=3306
DB_NAME=backend_db
DB_USERNAME=root
DB_PASSWORD=root_password_secret_tcp

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# JWT (secret must be base64 encoded, expiration in milliseconds)
JWT_SECRET=<base64-encoded-256-bit-secret>
JWT_ACCESS_TOKEN_EXPIRATION=3600000      # 1 hour
JWT_REFRESH_TOKEN_EXPIRATION=604800000   # 7 days

# MinIO
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=admin
MINIO_SECRET_KEY=123456789
MINIO_BUCKET=face-images

# Face Recognition Service
FACE_SERVICE_URL=http://face-recognition-service:5000
```

## Example API Usage

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "SecurePass123"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/user/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123"
  }'
# Response: { "data": { "access_token": {...}, "refresh_token": {...} } }

# Get profile (with JWT)
curl -X POST http://localhost:8080/profile \
  -H "Authorization: Bearer <access_token>"

# Register face (with JWT)
curl -X POST http://localhost:8080/api/v1/face/register-identity \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{"image_data": "<base64-encoded-image>"}'
```

## Troubleshooting

```bash
# Check all services status
docker-compose ps

# Check service health
curl http://localhost:8080/ping
curl http://localhost/health      # nginx health

# View logs
docker-compose logs -f                    # All services
docker-compose logs -f backend-service    # Backend only
docker-compose logs -f mysql              # Database only

# Database connection test
docker-compose exec mysql mysql -u root -proot_password_secret_tcp -e "SHOW DATABASES;"

# Redis connection test
docker-compose exec redis redis-cli ping

# MinIO health check
curl http://localhost:9000/minio/health/live

# Restart a specific service
docker-compose restart backend-service

# Rebuild and restart
docker-compose up -d --build backend-service

# Clean up and start fresh
docker-compose down -v
docker-compose up -d
```

## Documentation

For comprehensive documentation including:
- Technology stack details
- Security implementation (JWT, password hashing)
- Database design
- Modular monolith architecture
- Best practices
- Scaling strategies

See: **[CLAUDE.md](./CLAUDE.md)**

## Migration Notes

This project uses a **Modular Monolith** architecture (package-by-feature). The deprecated microservices (Go auth-service, profile-service, ai-backend-service, and original face-reg-engine) have been archived in `_archive/` folder for reference.

## License

MIT License
