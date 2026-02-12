# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a polyglot face recognition microservices system with services written in Go, Python, Java, and React/TypeScript. Services communicate via HTTP REST and gRPC, backed by MySQL, Redis, MinIO object storage, and a FEK (Fluent-bit, Elasticsearch, Kibana) logging stack.

## Common Commands

### Docker Compose (Full Stack)
```bash
# Build all services
docker-compose build

# Start all services
docker-compose up

# Start specific services
docker-compose up -d mysql redis face-regconition-service

# View logs
docker-compose logs <service-name>
```

### Go Services (ai-backend-service, auth-service, profile-service)
```bash
# Run tests
go test ./...

# Run single test
go test ./path/to/package -run TestFunctionName

# Build
go build -o app main.go

# Run locally (ai-backend-service)
cd ai-backend-service && make run
```

### Python Service (face-regconition-service)
```bash
cd face-regconition-service
pytest                           # Run all tests
pytest tests/test_handlers.py    # Run specific test file
pytest -k "test_name"            # Run tests matching pattern
```

### Java Service (face-reg-engine)
```bash
cd face-reg-engine
./gradlew build
./gradlew bootJar
./gradlew test
```

### Frontend (gui-app)
```bash
cd gui-app
pnpm install
pnpm dev       # Development server
pnpm build     # Production build
pnpm lint      # Run ESLint
```

### FEK Logging Stack
```bash
./setup-fek.sh                              # Setup logging stack
docker compose up -d elasticsearch kibana fluent-bit  # Start logging services
```

## Architecture

### Service Structure

| Service | Language | Port | Purpose |
|---------|----------|------|---------|
| ai-backend-service | Go/Gin | 3000 | Gateway, AI operations |
| auth-service | Go/Gin | 3100 (HTTP), 3101 (gRPC) | JWT authentication |
| user-service (profile-service) | Go/Gin | 3200 (HTTP), 3201 (gRPC) | User profile management |
| face-regconition-service | Python/Flask | 5000 | Core face recognition (PyTorch/RetinaFace) |
| face-reg-engine | Java/Spring Boot | 8080 | Face recognition REST API |
| app-fe (gui-app) | React/Vite | 80 | Web frontend |

### Inter-Service Communication
- **gRPC**: auth-service <-> user-service for internal communication
- **HTTP REST**: External APIs and service-to-service calls
- Protocol buffer definitions in `proto/` directories within Go services

### Go Service Pattern
All Go services follow this structure:
```
service-name/
├── main.go           # Entry point, calls cmd.Execute()
├── cmd/root.go       # Cobra CLI setup, service initialization
├── service/          # Business logic
├── models/           # Data structures
├── middleware/       # HTTP middleware
├── helpers/          # Utilities
└── proto/            # gRPC definitions (buf.yaml, buf.gen.yaml)
```

Uses `viettranx/service-context` for dependency injection (Gin, GORM, config).

### Database Schema
Each service maintains its own database:
- `auth-db` - Authentication tokens and sessions
- `user-db` - User profiles
- `ai-backend-service-db` - AI service data
- `face-db` - Face recognition engine data (Java service)

### Data Storage
- **MySQL 8.0**: Primary data store
- **Redis 7.4**: Caching, sessions, token management
- **MinIO**: Object storage for face images (S3-compatible)

## Key Dependencies

- **Go**: Gin (HTTP), GORM (ORM), golang-jwt/jwt v5, gRPC, testify (testing)
- **Python**: Flask, PyTorch, OpenCV, RetinaFace
- **Java**: Spring Boot 3.3.4, JPA, MinIO client
- **Frontend**: React 18, Vite, Tailwind CSS, react-hook-form, yup

## Service Ports Reference

| Service | HTTP | gRPC | Additional |
|---------|------|------|------------|
| Frontend | 80 | - | - |
| AI Backend | 3000 | - | - |
| Auth | 3100 | 3101 | - |
| User | 3200 | 3201 | - |
| Face Recognition (Python) | 5000 | - | - |
| Face Engine (Java) | 8080 | - | - |
| MySQL | 3306 | - | - |
| Redis | 6379 | - | - |
| MinIO | 9000 | - | Console: 9001 |
| Elasticsearch | 9200 | - | Transport: 9300 |
| Kibana | 5601 | - | - |
| Fluent-bit | 24224 | - | Metrics: 2020 |

## Git Submodules

Each service is a separate git submodule:
- ai-backend-service, auth-service, profile-service, face-reg-engine, face-regconition-service, gui-app
