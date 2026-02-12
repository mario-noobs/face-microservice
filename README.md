# Face Recognition System

A production-ready face recognition system with a simplified, scalable architecture.

## Architecture

```
┌─────────────────┐     ┌─────────────────────┐     ┌────────────────────┐
│   Frontend      │────▶│   Backend Service   │────▶│  Face Recognition  │
│   (React)       │     │   (Java/Spring)     │     │     (Python)       │
│   Port: 80      │     │   Port: 8080        │     │   Port: 5000       │
└─────────────────┘     └─────────────────────┘     └────────────────────┘
                               │
                    ┌──────────┼──────────┐
                    ▼          ▼          ▼
              ┌─────────┐ ┌─────────┐ ┌─────────┐
              │  MySQL  │ │  Redis  │ │  MinIO  │
              │  :3306  │ │  :6379  │ │  :9000  │
              └─────────┘ └─────────┘ └─────────┘
```

## Quick Start

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
| **backend-service** | Java Spring Boot 3.3 | 8080 | API Gateway, Auth, User Management, Face Operations |
| **face-recognition-service** | Python Flask + PyTorch | 5000 | AI Face Detection & Recognition |
| **gui-app** | React + TypeScript | 80 | Web Frontend |
| **mysql** | MySQL 8.0 | 3306 | Primary Database |
| **redis** | Redis 7.4 | 6379 | Token Blacklist & Cache |
| **minio** | MinIO | 9000 | Object Storage (S3-compatible) |

## API Endpoints

### Authentication (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/user/authenticate` | Login with email/password |
| POST | `/api/v1/user/register` | Register new user |
| POST | `/api/v1/user/logout` | Logout (blacklist token) |
| POST | `/api/v1/user/refresh` | Refresh access token |

### Protected Endpoints (Require JWT)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/profile` | Get current user profile |
| POST | `/api/v1/face/register-identity` | Register face |
| POST | `/api/v1/face/recognize-identity` | Recognize face |
| POST | `/api/v1/face/delete-identity` | Delete face data |
| GET | `/api/v1/face/is-registered` | Check if user has registered face |
| GET | `/api/v1/audit/all` | Get audit logs |

## Project Structure

```
face-microservice/
├── backend-service/          # Java Spring Boot monolith
│   ├── src/main/java/
│   │   └── com/mario/backend/
│   │       ├── controller/   # REST controllers
│   │       ├── service/      # Business logic
│   │       ├── security/     # JWT authentication
│   │       ├── entity/       # JPA entities
│   │       └── repository/   # Data access
│   ├── build.gradle
│   └── Dockerfile
├── face-regconition-service/ # Python AI service
├── gui-app/                  # React frontend
├── logging/                  # Fluent-bit configs
├── _archive/                 # Deprecated Go services
├── docker-compose.yml
├── nginx.conf
├── init.sql
├── CLAUDE.md                 # Detailed documentation
└── README.md
```

## Development

```bash
# Backend (Java)
cd backend-service
gradle bootRun

# Frontend (React)
cd gui-app
pnpm install && pnpm dev

# AI Service (Python)
cd face-regconition-service
pip install -r requirements.txt
python app.py
```

## Environment Variables

```bash
# Backend Service
DB_HOST=mysql                 # Database host
DB_PORT=3306                  # Database port
DB_NAME=backend_db            # Database name
DB_USERNAME=root              # Database user
DB_PASSWORD=<password>        # Database password
REDIS_HOST=redis              # Redis host
REDIS_PORT=6379               # Redis port
JWT_SECRET=<256-bit-secret>   # JWT signing key
JWT_ACCESS_TOKEN_EXPIRY=15m   # Access token TTL
JWT_REFRESH_TOKEN_EXPIRY=7d   # Refresh token TTL
MINIO_URL=http://minio        # MinIO URL
MINIO_PORT=9000               # MinIO port
MINIO_ACCESS_KEY=admin        # MinIO access key
MINIO_SECRET_KEY=<password>   # MinIO secret key
MINIO_BUCKET=face-bucket      # MinIO bucket name
FACE_SERVICE_URL=http://face-recognition-service:5000
FACE_SERVICE_TIMEOUT=30000
```

## Documentation

For comprehensive documentation including technology details, security implementation, database design, best practices, and scaling strategies, see:

**[CLAUDE.md](./CLAUDE.md)** - Complete Technical Documentation

## Troubleshooting

```bash
# Check service health
curl http://localhost:8080/ping

# View logs
docker-compose logs -f backend-service

# Database connection test
docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"

# Redis connection test
docker-compose exec redis redis-cli ping
```

## Migration Notes

This project has been simplified from a microservices architecture to a monolithic backend. The deprecated services (Go auth-service, profile-service, ai-backend-service, and original face-reg-engine) have been archived in `_archive/` folder for reference.

## License

MIT License - see [LICENSE](LICENSE) for details.
