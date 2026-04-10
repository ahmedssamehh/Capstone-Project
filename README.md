# WorkHub - Multi-Tenant SaaS Backend

A comprehensive Spring Boot backend application for a multi-tenant SaaS platform with project and task management capabilities.

## 🚀 Quick Start

### Prerequisites
- **Java 17** or higher
- **PostgreSQL 12** or higher
- **Maven 3.6** or higher

### 1. Database Setup

```bash
# Create database
createdb workhub

# Or using psql
psql -U postgres
CREATE DATABASE workhub;
```

### 2. Configure Database

Update `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workhub
    username: postgres
    password: postgres
```

### 3. Run Application

```bash
# Build and run
mvn clean spring-boot:run
```

The application will start on **http://localhost:8080**

### 4. Test Data

The application automatically creates test data on startup:

**Admin User**:
- Email: `admin@demo.com`
- Password: `admin123`
- Role: TENANT_ADMIN

**Regular User**:
- Email: `user@demo.com`
- Password: `user123`
- Role: TENANT_USER

## 📋 API Endpoints

### Authentication

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@demo.com",
  "password": "admin123"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "admin@demo.com",
  "tenantId": 1,
  "role": "TENANT_ADMIN",
  "expiresIn": 86400000
}
```

#### Get Current User
```bash
GET /api/auth/me
Authorization: Bearer <token>
```

### Projects

#### Create Project
```bash
POST /api/projects
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Project Alpha",
  "description": "A new project",
  "projectKey": "ALPHA"
}
```

#### Get All Projects
```bash
GET /api/projects
Authorization: Bearer <token>
```

#### Get Project by ID
```bash
GET /api/projects/{id}
Authorization: Bearer <token>
```

### Tasks

#### Create Task for Project
```bash
POST /api/projects/{id}/tasks
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Implement feature",
  "description": "Feature description",
  "priority": "HIGH",
  "estimatedHours": 8
}
```

#### Update Task
```bash
PATCH /api/tasks/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "status": "IN_PROGRESS",
  "actualHours": 4
}
```

## 🧪 Sample Login & Usage

### Complete Workflow Example

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"admin123"}' \
  | jq -r '.token')

echo "Token: $TOKEN"

# 2. Get current user
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"

# 3. Create a project
PROJECT_ID=$(curl -s -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My First Project",
    "description": "Testing the API",
    "projectKey": "TEST"
  }' | jq -r '.id')

echo "Created Project ID: $PROJECT_ID"

# 4. Get all projects
curl -X GET http://localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN"

# 5. Get project by ID
curl -X GET http://localhost:8080/api/projects/$PROJECT_ID \
  -H "Authorization: Bearer $TOKEN"

# 6. Create a task
TASK_ID=$(curl -s -X POST http://localhost:8080/api/projects/$PROJECT_ID/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement authentication",
    "description": "Add JWT authentication",
    "priority": "HIGH",
    "estimatedHours": 16
  }' | jq -r '.id')

echo "Created Task ID: $TASK_ID"

# 7. Update task status
curl -X PATCH http://localhost:8080/api/tasks/$TASK_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS",
    "actualHours": 8
  }'

# 8. Get task
curl -X GET http://localhost:8080/api/tasks/$TASK_ID \
  -H "Authorization: Bearer $TOKEN"
```

## 📚 API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/login` | Login and get JWT token | No |
| GET | `/api/auth/me` | Get current user profile | Yes |

### Project Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/projects` | Create project | Yes |
| GET | `/api/projects` | Get all projects | Yes |
| GET | `/api/projects/{id}` | Get project by ID | Yes |
| POST | `/api/projects/{id}/tasks` | Create task for project | Yes |

### Task Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/tasks/{id}` | Get task by ID | Yes |
| PATCH | `/api/tasks/{id}` | Update task (partial) | Yes |
| DELETE | `/api/tasks/{id}` | Delete task | Yes |

### Test Endpoints (Development Only)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/test/transaction/rollback` | Test transaction rollback |
| GET | `/api/test/transaction/verify` | Verify rollback |
| GET | `/api/test/transaction/instructions` | Get test instructions |

## 🔐 Sample Login

### Using curl

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@demo.com",
    "password": "admin123"
  }'
```

### Using Postman

1. **Method**: POST
2. **URL**: `http://localhost:8080/api/auth/login`
3. **Headers**: `Content-Type: application/json`
4. **Body** (raw JSON):
```json
{
  "email": "admin@demo.com",
  "password": "admin123"
}
```

### Response

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInRlbmFudElkIjoxLCJyb2xlIjoiVEVOQU5UX0FETUlOIiwic3ViIjoiYWRtaW5AZGVtby5jb20iLCJpYXQiOjE3MDUzMjAwMDAsImV4cCI6MTcwNTQwNjQwMH0.xxx",
  "type": "Bearer",
  "userId": 1,
  "email": "admin@demo.com",
  "tenantId": 1,
  "role": "TENANT_ADMIN",
  "expiresIn": 86400000
}
```

### Using the Token

Add the token to all subsequent requests:

```bash
curl -X GET http://localhost:8080/api/projects \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## 🏃 How to Run

### Option 1: Maven (Development)

```bash
# Clean and run
mvn clean spring-boot:run

# Run with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with specific port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

### Option 2: JAR File (Production)

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/workhub-0.0.1-SNAPSHOT.jar

# Run with profile
java -jar target/workhub-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Option 3: Docker (Optional)

```bash
# Build image
docker build -t workhub:latest .

# Run container
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/workhub \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=postgres \
  workhub:latest
```

### Option 4: Docker Compose

```bash
# Start all services (app + database)
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## 🔧 Configuration

### Application Properties

**File**: `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: workhub
  
  datasource:
    url: jdbc:postgresql://localhost:5432/workhub
    username: postgres
    password: postgres
  
  jpa:
    hibernate:
      ddl-auto: update  # Creates/updates tables automatically
    show-sql: true      # Shows SQL in logs

server:
  port: 8080

jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: 86400000  # 24 hours
```

### Environment Variables

```bash
# Database
export DATABASE_URL=jdbc:postgresql://localhost:5432/workhub
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres

# JWT
export JWT_SECRET=your-secret-key-here

# Run
mvn spring-boot:run
```

## 🏗️ Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    Controller Layer                      │
│  - REST endpoints                                        │
│  - Request/Response handling                             │
│  - Input validation (@Valid)                             │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                     Service Layer                        │
│  - Business logic                                        │
│  - Transaction management (@Transactional)               │
│  - Tenant isolation (TenantContext)                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   Repository Layer                       │
│  - Data access (Spring Data JPA)                         │
│  - Tenant-filtered queries                               │
│  - Database operations                                   │
└─────────────────────────────────────────────────────────┘
```

### Multi-Tenant Security

```
Request with JWT
    ↓
TenantContextFilter extracts tenantId from JWT
    ↓
TenantContext.setTenantId(tenantId)
    ↓
Service uses TenantContext.getTenantId()
    ↓
Repository filters by tenantId
    ↓
Only tenant's data returned
```

## 🧪 Testing

### Manual Testing

1. **Start application**: `mvn spring-boot:run`
2. **Login**: Get JWT token
3. **Test endpoints**: Use curl or Postman
4. **Verify**: Check database

### Transaction Rollback Test

```bash
# Test transaction rollback
curl -X POST http://localhost:8080/api/test/transaction/rollback \
  -H "Authorization: Bearer $TOKEN"

# Expected: 500 error (intentional)

# Verify nothing saved
curl -X GET "http://localhost:8080/api/test/transaction/verify?projectId=X&taskId=Y" \
  -H "Authorization: Bearer $TOKEN"

# Expected: rollbackSuccessful: true
```

## 📖 Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming language |
| Spring Boot | 3.3.5 | Application framework |
| Spring Data JPA | 3.3.5 | Data access |
| Spring Security | 3.3.5 | Authentication & authorization |
| PostgreSQL | 12+ | Database |
| Lombok | Latest | Code generation |
| JWT (jjwt) | 0.11.5 | Token authentication |
| Maven | 3.6+ | Build tool |

## 🔒 Security Features

### JWT Authentication
- Token-based authentication
- Includes `tenantId` and `role` in claims
- Automatic tenant context setup
- 24-hour token expiration

### Tenant Isolation
- All operations automatically filtered by tenant
- No cross-tenant access possible
- ThreadLocal-based tenant context
- Automatic cleanup after request

### Password Security
- BCrypt password hashing
- No plain text passwords stored
- Secure password validation

## 📝 Sample Requests

### 1. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@demo.com",
    "password": "admin123"
  }'
```

### 2. Create Project

```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Project",
    "description": "Project description",
    "projectKey": "PROJ"
  }'
```

### 3. Get Projects

```bash
curl -X GET http://localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Create Task

```bash
curl -X POST http://localhost:8080/api/projects/1/tasks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Implement feature",
    "description": "Feature description",
    "priority": "HIGH",
    "estimatedHours": 8
  }'
```

### 5. Update Task

```bash
curl -X PATCH http://localhost:8080/api/tasks/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PROGRESS",
    "actualHours": 4
  }'
```

## 🎯 Features

### Multi-Tenancy
- ✅ Complete tenant isolation
- ✅ Automatic tenant context from JWT
- ✅ No cross-tenant data access
- ✅ ThreadLocal-based context management

### Entities
- ✅ **Tenant**: Organization/company
- ✅ **User**: Users with roles (TENANT_ADMIN, TENANT_USER)
- ✅ **Project**: Projects with status tracking
- ✅ **Task**: Tasks with priority, status, and assignments

### Security
- ✅ JWT authentication
- ✅ BCrypt password hashing
- ✅ Role-based access control
- ✅ Tenant-aware security filter

### Data Management
- ✅ Spring Data JPA repositories
- ✅ Tenant-filtered queries
- ✅ Transaction management
- ✅ Optimistic locking (@Version)

### API Features
- ✅ RESTful endpoints
- ✅ DTO pattern (not exposing entities)
- ✅ Input validation (@Valid)
- ✅ Consistent error responses
- ✅ Global exception handling

## 📂 Project Structure

```
src/main/java/com/workhub/
├── entity/              # JPA entities
│   ├── Tenant.java
│   ├── User.java
│   ├── Project.java
│   └── Task.java
├── dto/                 # Data Transfer Objects
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── CreateProjectRequest.java
│   ├── ProjectResponse.java
│   ├── CreateTaskRequest.java
│   ├── TaskResponse.java
│   └── UpdateTaskRequest.java
├── repository/          # Spring Data JPA repositories
│   ├── TenantRepository.java
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   └── TaskRepository.java
├── service/             # Business logic
│   ├── ProjectService.java
│   └── TaskService.java
├── controller/          # REST controllers
│   ├── AuthController.java
│   ├── ProjectController.java
│   └── TaskController.java
├── security/            # Security components
│   ├── SecurityConfig.java
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── TenantContext.java
│   └── TenantContextFilter.java
├── exception/           # Exception handling
│   └── GlobalExceptionHandler.java
├── config/              # Configuration
│   └── DataInitializer.java
└── WorkHubApplication.java
```

## 🛠️ Development

### Build

```bash
# Clean build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run tests only
mvn test
```

### Run

```bash
# Development mode (with auto-reload)
mvn spring-boot:run

# With debug
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### Database

```bash
# Connect to database
psql -U postgres -d workhub

# View tables
\dt

# View projects
SELECT * FROM projects;

# View tasks
SELECT * FROM tasks;

# View users
SELECT * FROM users;
```

## 🐛 Troubleshooting

### Application won't start

**Check**:
- PostgreSQL is running
- Database `workhub` exists
- Database credentials are correct in `application.yml`

### 401 Unauthorized

**Solution**:
- Login to get a valid JWT token
- Include token in Authorization header: `Bearer <token>`

### 404 Not Found

**Solution**:
- Verify resource exists
- Check that resource belongs to your tenant
- Verify resource ID is correct

### Validation errors

**Solution**:
- Check request body format
- Ensure all required fields are present
- Verify field constraints (min/max length, etc.)

## 📊 Database Schema

### Tables

- `tenants` - Organizations/companies
- `users` - Users belonging to tenants
- `projects` - Projects belonging to tenants
- `tasks` - Tasks belonging to projects

### Key Relationships

```
Tenant (1) ─── (N) User
Tenant (1) ─── (N) Project
Project (1) ─── (N) Task
User (1) ─── (N) Project (as creator)
User (1) ─── (N) Task (as assignee)
```

### Indexes

All tables have indexes on:
- `tenant_id` - For efficient tenant filtering
- Foreign keys - For join performance
- Status fields - For filtering queries

## 🎓 Best Practices Implemented

1. ✅ **Clean Architecture** - Layered design
2. ✅ **DTO Pattern** - Separate API contracts from entities
3. ✅ **Validation** - Input validation with `@Valid`
4. ✅ **Transaction Management** - `@Transactional` for consistency
5. ✅ **Exception Handling** - Global exception handler
6. ✅ **Security** - JWT authentication with tenant isolation
7. ✅ **Logging** - Structured logging with SLF4J
8. ✅ **Separation of Concerns** - Clear layer boundaries

## 📄 License

This project is licensed under the MIT License.

## 👥 Test Accounts

| Email | Password | Role | Tenant |
|-------|----------|------|--------|
| admin@demo.com | admin123 | TENANT_ADMIN | Demo Company |
| user@demo.com | user123 | TENANT_USER | Demo Company |

## 🚀 Next Steps

After starting the application:

1. ✅ Login with test account
2. ✅ Create a project
3. ✅ Create tasks for the project
4. ✅ Update task status
5. ✅ Test transaction rollback

## 📞 Support

For issues or questions, check the documentation files:
- `TRANSACTION_ROLLBACK_TEST.md` - Transaction testing guide
- `SERVICE_LAYER_DOCUMENTATION.md` - Service layer details
- `TENANT_CONTEXT_USAGE.md` - Tenant context usage
- `REST_API_DOCUMENTATION.md` - Complete API reference
