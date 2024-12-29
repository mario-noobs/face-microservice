# Face Microservice

This repository contains a microservice architecture for a face recognition system, which is composed of multiple services including a face recognition engine, authentication, user profile management, and AI backend integration. The system also integrates with MySQL, Redis, Minio for storage, and includes services for monitoring, logging, and application UI. 

The following sections outline the architecture, setup, and usage of the system.

## Architecture Overview

The face recognition system is divided into several services that communicate via HTTP and gRPC, as well as utilize various data stores and external services. Below is the breakdown of each service:

### 1. **MySQL**
   - **Image**: `mysql:8.0`
   - **Ports**: `3306:3306`
   - **Environment Variables**: 
     - `MYSQL_ROOT_PASSWORD`: Root password for MySQL.
     - `MYSQL_DATABASE`: Database name for todo-list.
   - **Healthcheck**: Ensures MySQL is responsive before other services interact with it.
   - **Volumes**: 
     - Persistent data storage (`mysql` volume).
     - Initialization SQL scripts (`./data.sql`).

### 2. **Redis**
   - **Image**: `redis:7.4-alpine`
   - **Ports**: `6379:6379`
   - **Volumes**: Redis data storage (`redis` volume).
   - **Healthcheck**: Ensures Redis is available for caching and session management.

### 3. **Face Recognition Service**
   - **Build Directory**: `./face-regconition-service`
   - **Ports**: `5000:5000`
   - **Dependencies**: Relies on Redis being healthy for caching.
   - **Purpose**: Implements the core face recognition functionality.

### 4. **Face Recognition Engine**
   - **Build Directory**: `./face-reg-engine`
   - **Ports**: `8080:8080`
   - **Environment Variables**:
     - `jdbc:mysql://mysql:3306/FACE_ENGINE`: MySQL connection string.
     - Database credentials and configuration for Spring-based backend.
   - **Dependencies**: MySQL must be healthy to start.
   - **Purpose**: Provides the face recognition engine and API endpoints for face recognition operations.

### 5. **Auth Service**
   - **Build Directory**: `./auth-service`
   - **Environment Variables**:
     - Database connection string (`DB_DSN`) for MySQL.
     - `JWT_SECRET`: Secret for signing JWT tokens.
     - `GRPC_PORT`: Port for gRPC communication.
     - `GRPC_USER_ADDRESS`: gRPC endpoint for user service.
   - **Purpose**: Manages authentication and authorization, handling JWT generation.

### 6. **User Service**
   - **Build Directory**: `./profile-service`
   - **Environment Variables**:
     - MySQL connection string (`DB_DSN`) for user data.
     - `GRPC_PORT`: Port for gRPC communication.
     - `GRPC_AUTH_ADDRESS`: Endpoint for auth-service.
   - **Purpose**: Manages user profiles, including storing and retrieving user data.

### 7. **AI Backend Service**
   - **Build Directory**: `./ai-backend-service`
   - **Ports**: `3000:3000`
   - **Environment Variables**:
     - `DB_DSN`: Database connection string for the AI backend.
     - `JWT_SECRET`: Secret for JWT authentication.
     - gRPC connections to other services (Auth, Profile).
   - **Purpose**: Handles AI-based backend operations like processing image data or managing AI models.

### 8. **App Frontend (GUI)**
   - **Build Directory**: `./gui-app`
   - **Ports**: `80:80`
   - **Purpose**: A web-based front-end interface for interacting with the face recognition service.

### 9. **MinIO**
   - **Image**: `quay.io/minio/minio`
   - **Ports**: `9000:9000`, `9001:9001`
   - **Environment Variables**:
     - `MINIO_ROOT_USER`: The MinIO root user.
     - `MINIO_ROOT_PASSWORD`: MinIO root password.
   - **Command**: Configures MinIO server to use `/data` directory for file storage.
   - **Purpose**: Object storage for the system, handling file uploads and downloads.

### Optional Services (Commented Out)

- **Gateway (Tyk Gateway)**: API gateway for managing access to services.
- **Grafana**: Monitoring service for visualization of metrics.
- **Prometheus**: Metrics collector for monitoring system health.

## Installation & Setup

### Prerequisites
Ensure the following software is installed:
- Docker
- Docker Compose

### Steps
1. Clone the repository:

   ```bash
   git clone https://github.com/mario-noobs/face-microservice.git
   cd face-microservice
   ```

2. Build the services using Docker Compose:

   ```bash
   docker-compose build
   ```

3. Start the services:

   ```bash
   docker-compose up
   ```

4. Access the following services via the exposed ports:
   - **Face Recognition Service**: `http://localhost:5000`
   - **Face Recognition Engine**: `http://localhost:8080`
   - **User Service**: `http://localhost:3200`
   - **Auth Service**: `http://localhost:3100`
   - **Frontend App**: `http://localhost:80`

5. The system will automatically initialize the databases with the data from `data.sql`.

## Health Checks
The system includes health checks for each service:
- MySQL, Redis, Face Recognition, and other services ensure proper startup and availability before dependencies start.

## Data Persistence
- **MySQL**: Persistent volume for database storage.
- **Redis**: Persistent volume for caching.
- **MinIO**: Persistent file storage for object storage.

## Notes

- **JWT Secrets**: Change the `JWT_SECRET` environment variable to something more secure before deploying in production.
- **Sensitive Data**: Do not expose production credentials in public repositories. Ensure `.env` files or Docker secrets are used for production environments.

## Troubleshooting

- If any services fail to start, check the logs using:
  ```bash
  docker-compose logs <service-name>
  ```
- If the database isn't initialized, ensure that the `data.sql` file is correctly placed and formatted.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
