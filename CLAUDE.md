# Face Recognition System - Technical Documentation

> **Claude Code Guidance File** - This document provides comprehensive guidance for AI assistants and developers working with this codebase.

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Project Structure](#project-structure)
5. [Getting Started](#getting-started)
6. [API Reference](#api-reference)
7. [Security Implementation](#security-implementation)
8. [Database Design](#database-design)
9. [Best Practices](#best-practices)
10. [Scaling Strategies](#scaling-strategies)
11. [Troubleshooting](#troubleshooting)

---

## Project Overview

A production-ready face recognition system with a clean, scalable architecture:

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Frontend** | React + TypeScript | Single-page application |
| **Backend** | Java Spring Boot 3.3 | API, Auth, Business Logic |
| **AI Service** | Python Flask + PyTorch | Face detection & recognition |
| **Database** | MySQL 8.0 | Persistent data storage |
| **Cache** | Redis 7.4 | Token blacklist, sessions |
| **Object Storage** | MinIO | Face images (S3-compatible) |

---

## Architecture

### High-Level Architecture

```
                                    ┌─────────────────────────────────────────┐
                                    │              Load Balancer              │
                                    │            (nginx / AWS ALB)            │
                                    └─────────────────┬───────────────────────┘
                                                      │
                    ┌─────────────────────────────────┼─────────────────────────────────┐
                    │                                 │                                 │
                    ▼                                 ▼                                 ▼
          ┌─────────────────┐             ┌─────────────────┐             ┌─────────────────┐
          │    Frontend     │             │    Frontend     │             │    Frontend     │
          │   (React SPA)   │             │   (React SPA)   │             │   (React SPA)   │
          │   Port: 80      │             │   Port: 80      │             │   Port: 80      │
          └────────┬────────┘             └────────┬────────┘             └────────┬────────┘
                   │                               │                               │
                   └───────────────────────────────┼───────────────────────────────┘
                                                   │
                                    ┌──────────────┴──────────────┐
                                    │      Backend Service        │
                                    │    (Java Spring Boot)       │
                                    │        Port: 8080           │
                                    │  ┌───────────────────────┐  │
                                    │  │ • Authentication      │  │
                                    │  │ • User Management     │  │
                                    │  │ • Face Operations     │  │
                                    │  │ • Audit Logging       │  │
                                    │  └───────────────────────┘  │
                                    └──────────────┬──────────────┘
                                                   │
                    ┌──────────────────────────────┼──────────────────────────────┐
                    │                              │                              │
                    ▼                              ▼                              ▼
          ┌─────────────────┐          ┌─────────────────┐          ┌─────────────────┐
          │  Face Service   │          │     MySQL       │          │     Redis       │
          │    (Python)     │          │   (Primary DB)  │          │    (Cache)      │
          │   Port: 5000    │          │   Port: 3306    │          │   Port: 6379    │
          └─────────────────┘          └─────────────────┘          └─────────────────┘
                   │
                   ▼
          ┌─────────────────┐
          │     MinIO       │
          │ (Object Store)  │
          │   Port: 9000    │
          └─────────────────┘
```

### Request Flow

```
1. User Request → nginx (port 80)
2. nginx routes /api/* → Backend Service (port 8080)
3. Backend validates JWT token via Spring Security
4. Backend processes request:
   - Auth: Validates credentials, generates JWT
   - Face: Calls Python AI service, stores in MinIO
   - Profile: Queries MySQL database
5. Response returned to user
```

---

## Technology Stack

### Backend Service (Java)

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 LTS | Runtime |
| Spring Boot | 3.3.4 | Application framework |
| Spring Security | 6.x | Authentication & Authorization |
| Spring Data JPA | 3.x | ORM / Database access |
| Spring Data Redis | 3.x | Redis client |
| JJWT | 0.12.5 | JWT token handling |
| HikariCP | 5.x | Connection pooling |
| MinIO SDK | 8.5.7 | Object storage client |
| OkHttp | 4.12.0 | HTTP client for AI service |
| Lombok | 1.18.x | Boilerplate reduction |
| Jakarta Validation | 3.x | Request validation |

### AI Service (Python)

| Technology | Version | Purpose |
|------------|---------|---------|
| Python | 3.10+ | Runtime |
| Flask | 2.x | Web framework |
| PyTorch | 2.x | Deep learning framework |
| OpenCV | 4.x | Image processing |
| RetinaFace | - | Face detection model |
| ArcFace | - | Face recognition model |

### Frontend (React)

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.x | UI framework |
| TypeScript | 5.x | Type safety |
| Vite | 5.x | Build tool |
| Tailwind CSS | 3.x | Styling |
| React Hook Form | 7.x | Form handling |
| Yup | 1.x | Validation |
| Axios | 1.x | HTTP client |

### Infrastructure

| Technology | Version | Purpose |
|------------|---------|---------|
| Docker | 24.x | Containerization |
| Docker Compose | 2.x | Container orchestration |
| nginx | 1.25 | Reverse proxy / Static files |
| MySQL | 8.0 | Relational database |
| Redis | 7.4 | In-memory cache |
| MinIO | Latest | S3-compatible storage |

---

## Project Structure

```
face-microservice/
├── backend-service/                 # Java Spring Boot modular monolith
│   ├── build.gradle                # Gradle build configuration
│   ├── Dockerfile                  # Container build
│   └── src/main/
│       ├── java/com/mario/backend/
│       │   ├── BackendApplication.java
│       │   │
│       │   ├── common/                      # Shared utilities
│       │   │   ├── dto/
│       │   │   │   └── ApiResponse.java     # Standard API response wrapper
│       │   │   └── exception/
│       │   │       ├── ApiException.java    # Custom exception
│       │   │       └── GlobalExceptionHandler.java
│       │   │
│       │   ├── auth/                        # Authentication module
│       │   │   ├── controller/
│       │   │   │   └── AuthController.java  # /api/v1/user/*
│       │   │   ├── service/
│       │   │   │   ├── AuthService.java
│       │   │   │   └── TokenBlacklistService.java
│       │   │   ├── security/
│       │   │   │   ├── JwtTokenProvider.java
│       │   │   │   ├── JwtAuthenticationFilter.java
│       │   │   │   └── AuthenticatedUser.java
│       │   │   ├── repository/
│       │   │   │   └── AuthRepository.java
│       │   │   ├── entity/
│       │   │   │   └── Auth.java
│       │   │   └── dto/
│       │   │       ├── LoginRequest.java
│       │   │       ├── RegisterRequest.java
│       │   │       ├── TokenResponse.java
│       │   │       ├── RefreshTokenRequest.java
│       │   │       └── LogoutRequest.java
│       │   │
│       │   ├── users/                       # User management module
│       │   │   ├── controller/
│       │   │   │   └── ProfileController.java  # /profile
│       │   │   ├── service/
│       │   │   │   └── UserService.java
│       │   │   ├── repository/
│       │   │   │   └── UserRepository.java
│       │   │   ├── entity/
│       │   │   │   └── User.java
│       │   │   └── dto/
│       │   │       ├── UserResponse.java
│       │   │       └── UpdateProfileRequest.java
│       │   │
│       │   ├── face/                        # Face recognition module
│       │   │   ├── controller/
│       │   │   │   └── FaceController.java  # /api/v1/face/*
│       │   │   ├── service/
│       │   │   │   ├── FaceService.java
│       │   │   │   └── MinioService.java
│       │   │   ├── repository/
│       │   │   │   ├── FaceFeatureRepository.java
│       │   │   │   └── FaceImageRepository.java
│       │   │   ├── entity/
│       │   │   │   ├── FaceFeature.java
│       │   │   │   └── FaceImage.java
│       │   │   └── dto/
│       │   │       ├── FaceRegisterRequest.java
│       │   │       ├── FaceRecognizeRequest.java
│       │   │       └── FaceResponse.java
│       │   │
│       │   ├── audit/                       # Audit logging module
│       │   │   ├── controller/
│       │   │   │   └── AuditController.java # /api/v1/audit/*
│       │   │   ├── service/
│       │   │   │   └── AuditService.java
│       │   │   ├── repository/
│       │   │   │   └── AuditLogRepository.java
│       │   │   ├── entity/
│       │   │   │   └── AuditLog.java
│       │   │   └── dto/
│       │   │       ├── AuditLogResponse.java
│       │   │       └── PageResponse.java
│       │   │
│       │   └── gateway/                     # Gateway/config module
│       │       ├── config/
│       │       │   ├── SecurityConfig.java
│       │       │   ├── MinioConfig.java
│       │       │   ├── RedisConfig.java
│       │       │   └── AsyncConfig.java
│       │       ├── filter/
│       │       │   └── AuditLoggingFilter.java
│       │       └── controller/
│       │           └── HealthController.java  # /ping
│       │
│       └── resources/
│           └── application.yml              # Configuration
│
├── face-regconition-service/        # Python AI service
│   ├── app/
│   │   ├── handlers/               # Request handlers
│   │   ├── models/                 # ML models
│   │   └── utils/                  # Utilities
│   ├── requirements.txt
│   └── Dockerfile
│
├── gui-app/                         # React frontend
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/               # API clients
│   │   └── hooks/
│   ├── package.json
│   └── Dockerfile
│
├── logging/                         # Fluent-bit configs
├── _archive/                        # Deprecated services
├── docker-compose.yml               # Container orchestration
├── nginx.conf                       # Reverse proxy config
├── init.sql                         # Database initialization
└── CLAUDE.md                        # This file
```

---

## Getting Started

### Prerequisites

- Docker 24+ and Docker Compose 2+
- 8GB RAM minimum (16GB recommended for AI service)
- 20GB disk space

### Quick Start

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
# Expected: {"data":"pong"}

# View logs
docker-compose logs -f backend-service
```

### Development Setup

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

### Environment Variables

```bash
# Backend Service
DB_HOST=mysql
DB_PORT=3306
DB_NAME=backend_db
DB_USERNAME=root
DB_PASSWORD=<secure-password>
REDIS_HOST=redis
REDIS_PORT=6379
JWT_SECRET=<256-bit-secret>           # IMPORTANT: Change in production
JWT_ACCESS_TOKEN_EXPIRY=15m
JWT_REFRESH_TOKEN_EXPIRY=7d
MINIO_URL=http://minio
MINIO_PORT=9000
MINIO_ACCESS_KEY=admin
MINIO_SECRET_KEY=<secure-password>
MINIO_BUCKET=face-bucket
FACE_SERVICE_URL=http://face-recognition-service:5000
FACE_SERVICE_TIMEOUT=30000
```

---

## API Reference

### Authentication Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/user/authenticate` | No | Login with email/password |
| POST | `/api/v1/user/register` | No | Register new user |
| POST | `/api/v1/user/logout` | No | Invalidate access token |
| POST | `/api/v1/user/refresh` | No | Get new access token |

### User Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/profile` | Yes | Get current user profile |
| GET | `/api/v1/user/{id}` | Yes | Get user by ID |

### Face Recognition Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/face/register-identity` | Yes | Register face |
| POST | `/api/v1/face/recognize-identity` | Yes | Recognize face |
| POST | `/api/v1/face/delete-identity` | Yes | Delete face data |
| GET | `/api/v1/face/is-registered` | Yes | Check registration |

### Audit Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/audit/all` | Yes | List audit logs |

### Example Requests

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

# Register face (with JWT)
curl -X POST http://localhost:8080/api/v1/face/register-identity \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <access_token>" \
  -d '{
    "imageBase64": "<base64-encoded-image>"
  }'
```

---

## Security Implementation

### JWT Token Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                        JWT Token Flow                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Login Request                                                │
│     POST /api/v1/user/authenticate                              │
│     { email, password }                                          │
│                                                                  │
│  2. Server validates credentials                                 │
│     - Query auth table by email                                  │
│     - Verify: bcrypt(salt + "." + password) == stored_hash      │
│                                                                  │
│  3. Generate tokens                                              │
│     Access Token:  { sub: userId, exp: 15min, jti: uuid }       │
│     Refresh Token: { sub: userId, exp: 7days, jti: uuid_refresh }│
│                                                                  │
│  4. Return tokens to client                                      │
│                                                                  │
│  5. Client stores tokens (localStorage/httpOnly cookie)          │
│                                                                  │
│  6. Subsequent requests                                          │
│     Authorization: Bearer <access_token>                         │
│                                                                  │
│  7. Token refresh when access token expires                      │
│     POST /api/v1/user/refresh                                   │
│     { refresh_token: <refresh_token> }                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Password Security

```java
// Password hashing strategy
String salt = generateRandomSalt(16);  // 16 bytes, hex-encoded
String saltedPassword = salt + "." + plainPassword;
String hash = bcrypt.encode(saltedPassword);  // BCrypt with cost 10

// Stored in database:
// - salt: "a1b2c3d4e5f6g7h8..."
// - password: "$2a$10$..."
```

### Token Blacklisting (Redis)

```
Token Blacklist Strategy:
┌─────────────────────────────────────────────────┐
│ Key: blacklist:token:<token_string>             │
│ Value: "blacklisted"                            │
│ TTL: remaining token lifetime                   │
│                                                 │
│ User-level Blacklist (logout all devices):      │
│ Key: blacklist:user:<user_id>                   │
│ Value: timestamp of blacklist                   │
│ Check: if token_issued_at < blacklist_time      │
└─────────────────────────────────────────────────┘
```

### Security Best Practices Implemented

1. **Password Storage**: BCrypt with random salt per user
2. **JWT Signing**: HMAC-SHA256 with 256-bit secret
3. **Token Expiry**: Short-lived access tokens (15min)
4. **Token Revocation**: Redis-based blacklist with TTL
5. **CORS**: Configured for specific origins in production
6. **Input Validation**: Jakarta Bean Validation on all DTOs
7. **SQL Injection**: Prevented via JPA parameterized queries
8. **Error Handling**: Generic error messages (no stack traces)

---

## Database Design

### Entity Relationship Diagram

```
┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
│   users     │     │   auths     │     │  face_features  │
├─────────────┤     ├─────────────┤     ├─────────────────┤
│ id (PK)     │◄────│ user_id (FK)│     │ id (PK)         │
│ first_name  │     │ id (PK)     │     │ user_id         │
│ last_name   │     │ email (UK)  │     │ feature (TEXT)  │
│ email (UK)  │     │ password    │     │ flow            │
│ phone       │     │ salt        │     │ activate        │
│ gender      │     │ auth_type   │     │ create_date     │
│ system_role │     │ created_at  │     └─────────────────┘
│ status      │     └─────────────┘
│ created_at  │
└─────────────┘

┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  face_images    │     │  face_audits    │     │  audit_logs     │
├─────────────────┤     ├─────────────────┤     ├─────────────────┤
│ id (PK)         │     │ id (PK)         │     │ id (PK)         │
│ user_id         │     │ user_id         │     │ ip_address      │
│ file_name       │     │ prob            │     │ api_call        │
│ request_id      │     │ face_search     │     │ method          │
│ flow            │     │ flow            │     │ status          │
│ create_date     │     │ request_id      │     │ response_time   │
└─────────────────┘     │ code            │     │ user_id         │
                        │ message         │     │ time            │
                        └─────────────────┘     └─────────────────┘
```

### Indexes

```sql
-- Performance indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_auths_email ON auths(email);
CREATE INDEX idx_auths_user_id ON auths(user_id);
CREATE INDEX idx_face_features_user_id ON face_features(user_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_time ON audit_logs(time);
```

---

## Best Practices

### Modular Monolith Architecture

This project follows the **Modular Monolith** pattern (package-by-feature, not package-by-layer):

```
Traditional Layered (❌):              Modular/Feature-based (✅):
├── controller/                       ├── auth/
│   ├── AuthController.java          │   ├── controller/
│   ├── UserController.java          │   ├── service/
│   └── FaceController.java          │   ├── repository/
├── service/                          │   ├── entity/
│   ├── AuthService.java             │   └── dto/
│   ├── UserService.java             ├── users/
│   └── FaceService.java             │   ├── controller/
├── repository/                       │   ├── service/
│   └── ...                          │   └── ...
└── entity/                          └── face/
    └── ...                              └── ...
```

**Benefits:**
- **High cohesion**: Related code lives together
- **Clear boundaries**: Each module is self-contained
- **Easy to navigate**: Find all auth-related code in `auth/`
- **Microservice-ready**: Easy to extract modules later
- **Team ownership**: Different teams can own different modules

**Module Rules:**
1. Each module has its own controller, service, repository, entity, and dto packages
2. Modules communicate via service interfaces (not direct repository access)
3. Shared code lives in `common/` (exceptions, utilities, shared DTOs)
4. Cross-module dependencies should be minimized

### Code Organization

```
✅ DO:
- Use DTOs for API request/response (never expose entities)
- Validate all inputs with Jakarta Validation annotations
- Use constructor injection (not @Autowired on fields)
- Keep controllers thin, business logic in services
- Use transactions for multi-step operations
- Log at appropriate levels (DEBUG for dev, INFO for prod)
- Keep module boundaries clean (no circular dependencies)
- Place shared code in common/ module

❌ DON'T:
- Expose stack traces in API responses
- Store sensitive data in JWT payload
- Use String concatenation for SQL queries
- Catch Exception without handling specific types
- Hardcode configuration values
- Access another module's repository directly (use service)
- Create dependencies between unrelated modules
```

### API Design

```
✅ DO:
- Use consistent response format: { data: T, error: { code, message } }
- Version APIs: /api/v1/...
- Use appropriate HTTP status codes
- Document all endpoints
- Implement pagination for list endpoints

❌ DON'T:
- Return different structures for success/error
- Use verbs in URLs (use nouns + HTTP methods)
- Expose internal IDs when not necessary
```

### Error Handling

```java
// Global exception handler pattern
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return ResponseEntity
            .status(ex.getStatus())
            .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);  // Log full stack trace
        return ResponseEntity
            .status(500)
            .body(ApiResponse.error("500", "Internal server error"));  // Generic message
    }
}
```

---

## Scaling Strategies

### Horizontal Scaling

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         SCALING ARCHITECTURE                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  TIER 1: Load Balancer                                                   │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  nginx / AWS ALB / GCP Load Balancer                             │    │
│  │  - SSL termination                                               │    │
│  │  - Health checks                                                 │    │
│  │  - Request routing                                               │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                  │                                       │
│  TIER 2: Application Servers (Horizontal Scaling)                        │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                     │
│  │  Backend-1   │ │  Backend-2   │ │  Backend-N   │  ← Stateless        │
│  │  (8080)      │ │  (8080)      │ │  (8080)      │  ← Auto-scale       │
│  └──────────────┘ └──────────────┘ └──────────────┘                     │
│                                  │                                       │
│  TIER 3: Data Layer                                                      │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                     │
│  │  MySQL       │ │  Redis       │ │  MinIO       │                     │
│  │  Primary     │ │  Cluster     │ │  Cluster     │                     │
│  │  + Replicas  │ │              │ │              │                     │
│  └──────────────┘ └──────────────┘ └──────────────┘                     │
│                                                                          │
│  TIER 4: AI Service (GPU Scaling)                                        │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                     │
│  │  AI-GPU-1    │ │  AI-GPU-2    │ │  AI-GPU-N    │  ← GPU instances    │
│  │  (5000)      │ │  (5000)      │ │  (5000)      │  ← Queue-based      │
│  └──────────────┘ └──────────────┘ └──────────────┘                     │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Backend Service Scaling

```yaml
# docker-compose.yml for scaling
backend-service:
  image: backend-service:latest
  deploy:
    replicas: 3                    # Run 3 instances
    resources:
      limits:
        cpus: '2'
        memory: 2G
      reservations:
        cpus: '1'
        memory: 1G
    restart_policy:
      condition: on-failure
      max_attempts: 3
```

### Database Scaling

```
MySQL Scaling Strategy:
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│  Phase 1: Vertical Scaling                                   │
│  - Increase CPU, RAM, IOPS                                   │
│  - Optimize queries with EXPLAIN                             │
│  - Add proper indexes                                        │
│                                                              │
│  Phase 2: Read Replicas                                      │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐            │
│  │  Primary │────▶│ Replica 1│────▶│ Replica 2│            │
│  │  (Write) │     │  (Read)  │     │  (Read)  │            │
│  └──────────┘     └──────────┘     └──────────┘            │
│                                                              │
│  Phase 3: Sharding (if needed)                               │
│  - Shard by user_id                                          │
│  - Use consistent hashing                                    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Redis Scaling

```
Redis Cluster for High Availability:
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐            │
│  │ Master 1 │     │ Master 2 │     │ Master 3 │            │
│  └────┬─────┘     └────┬─────┘     └────┬─────┘            │
│       │                │                │                   │
│  ┌────▼─────┐     ┌────▼─────┐     ┌────▼─────┐            │
│  │ Replica  │     │ Replica  │     │ Replica  │            │
│  └──────────┘     └──────────┘     └──────────┘            │
│                                                              │
│  Data distribution:                                          │
│  - Token blacklist: distributed across shards               │
│  - Session data: hash slot based on key                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### AI Service Scaling

```
Face Recognition Service Scaling:
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│  Option 1: Message Queue Pattern                             │
│  ┌──────────┐    ┌──────────┐    ┌──────────────────┐       │
│  │ Backend  │───▶│  Queue   │───▶│ AI Workers (GPU) │       │
│  │ Service  │    │ (Redis/  │    │ - Process async  │       │
│  └──────────┘    │  RabbitMQ│    │ - Return via WS  │       │
│                  └──────────┘    └──────────────────┘       │
│                                                              │
│  Option 2: Kubernetes HPA                                    │
│  - GPU node pools                                            │
│  - Auto-scale based on queue length                          │
│  - Spot instances for cost optimization                      │
│                                                              │
│  Option 3: Serverless (AWS Lambda / GCP Cloud Functions)     │
│  - Pay per invocation                                        │
│  - Auto-scaling built-in                                     │
│  - Cold start consideration                                  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Caching Strategy

```
Cache Layers:
┌─────────────────────────────────────────────────────────────┐
│                                                              │
│  L1: Application Cache (Caffeine)                            │
│  - User profiles (5 min TTL)                                 │
│  - Configuration values                                      │
│                                                              │
│  L2: Distributed Cache (Redis)                               │
│  - Token blacklist                                           │
│  - Session data                                              │
│  - API response cache (optional)                             │
│                                                              │
│  L3: CDN (CloudFront / Cloudflare)                           │
│  - Static assets                                             │
│  - Face images (if public)                                   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Kubernetes Deployment

```yaml
# Example Kubernetes deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend-service
  template:
    spec:
      containers:
      - name: backend
        image: backend-service:latest
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        readinessProbe:
          httpGet:
            path: /ping
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /ping
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| 401 Unauthorized | Invalid/expired token | Check token expiry, refresh token |
| 500 on face register | AI service down | Check face-recognition-service logs |
| Slow responses | Database connection pool | Increase HikariCP pool size |
| Redis connection failed | Redis not ready | Check Redis health, connection string |

### Debug Commands

```bash
# View all service logs
docker-compose logs -f

# View specific service
docker-compose logs -f backend-service

# Check service health
curl http://localhost:8080/ping

# Database connection test
docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"

# Redis connection test
docker-compose exec redis redis-cli ping

# Check MinIO
curl http://localhost:9000/minio/health/live
```

### Performance Profiling

```bash
# Enable JVM profiling
JAVA_OPTS="-XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=/tmp/recording.jfr"

# Thread dump
jstack <pid> > thread_dump.txt

# Heap dump
jmap -dump:format=b,file=/tmp/heap.hprof <pid>
```

---

## Contributing

1. Follow the existing code style
2. Write tests for new features
3. Update documentation
4. Create descriptive commit messages

---

*Last updated: February 2026*
*Maintained by: Face Recognition Team*
