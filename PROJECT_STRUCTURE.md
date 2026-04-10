# Project Structure

## Complete Directory Tree

```
Capstone-Project/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── workhub/
│   │   │           ├── WorkHubApplication.java          # Main application entry point
│   │   │           │
│   │   │           ├── entity/                          # Domain Entities (JPA)
│   │   │           │   ├── Tenant.java                  # Tenant entity with relationships
│   │   │           │   ├── User.java                    # User entity with roles
│   │   │           │   ├── Project.java                 # Project entity
│   │   │           │   └── Task.java                    # Task entity
│   │   │           │
│   │   │           ├── dto/                             # Data Transfer Objects
│   │   │           │   ├── TenantResponse.java          # Tenant response DTO
│   │   │           │   ├── CreateTenantRequest.java     # Create tenant request
│   │   │           │   ├── UpdateTenantRequest.java     # Update tenant request
│   │   │           │   ├── UserDto.java                 # User response DTO
│   │   │           │   ├── CreateUserRequest.java       # Create user request
│   │   │           │   ├── UpdateUserRequest.java       # Update user request
│   │   │           │   ├── ProjectDto.java              # Project response DTO
│   │   │           │   ├── CreateProjectRequest.java    # Create project request
│   │   │           │   ├── UpdateProjectRequest.java    # Update project request
│   │   │           │   ├── TaskDto.java                 # Task response DTO
│   │   │           │   ├── CreateTaskRequest.java       # Create task request
│   │   │           │   └── UpdateTaskRequest.java       # Update task request
│   │   │           │
│   │   │           ├── repository/                      # Data Access Layer
│   │   │           │   ├── TenantRepository.java        # Tenant data access
│   │   │           │   ├── UserRepository.java          # User data access
│   │   │           │   ├── ProjectRepository.java       # Project data access
│   │   │           │   └── TaskRepository.java          # Task data access
│   │   │           │
│   │   │           ├── service/                         # Business Logic Layer
│   │   │           │   ├── TenantService.java           # Tenant service interface
│   │   │           │   ├── TenantServiceImpl.java       # Tenant service implementation
│   │   │           │   ├── UserService.java             # User service interface
│   │   │           │   ├── UserServiceImpl.java         # User service implementation
│   │   │           │   ├── ProjectService.java          # Project service interface
│   │   │           │   ├── ProjectServiceImpl.java      # Project service implementation
│   │   │           │   ├── TaskService.java             # Task service interface
│   │   │           │   └── TaskServiceImpl.java         # Task service implementation
│   │   │           │
│   │   │           ├── controller/                      # REST API Controllers
│   │   │           │   ├── HealthController.java        # Health check endpoint
│   │   │           │   ├── TenantController.java        # Tenant API endpoints
│   │   │           │   ├── UserController.java          # User API endpoints
│   │   │           │   ├── ProjectController.java       # Project API endpoints
│   │   │           │   └── TaskController.java          # Task API endpoints
│   │   │           │
│   │   │           ├── security/                        # Security & Authentication
│   │   │           │   ├── SecurityConfig.java          # Spring Security configuration
│   │   │           │   ├── TenantContext.java           # Tenant context holder
│   │   │           │   ├── TenantContextFilter.java     # Tenant identification filter
│   │   │           │   ├── JwtAuthenticationFilter.java # JWT authentication filter
│   │   │           │   └── JwtService.java              # JWT token service
│   │   │           │
│   │   │           ├── config/                          # Application Configuration
│   │   │           │   ├── ApplicationConfig.java       # General app configuration
│   │   │           │   ├── JpaConfig.java               # JPA configuration
│   │   │           │   └── PasswordEncoderConfig.java   # Password encoder bean
│   │   │           │
│   │   │           └── exception/                       # Exception Handling
│   │   │               ├── GlobalExceptionHandler.java  # Global exception handler
│   │   │               ├── ErrorResponse.java           # Error response DTO
│   │   │               └── ValidationErrorResponse.java # Validation error DTO
│   │   │
│   │   └── resources/
│   │       ├── application.yml                          # Main configuration
│   │       ├── application-dev.yml                      # Development profile
│   │       └── application-prod.yml                     # Production profile
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── workhub/
│                   └── WorkHubApplicationTests.java     # Application tests
│
├── pom.xml                                              # Maven dependencies
├── Dockerfile                                           # Docker image definition
├── docker-compose.yml                                   # Docker Compose for local dev
├── .gitignore                                           # Git ignore rules
├── README.md                                            # Project documentation
├── ARCHITECTURE.md                                      # Architecture documentation
├── DEVELOPMENT.md                                       # Development guide
└── PROJECT_STRUCTURE.md                                 # This file
```

## Package Structure Explanation

### Entity Layer (`entity/`)
**Purpose**: Domain models representing database tables

- **Tenant.java**: Multi-tenant organization entity
  - UUID identifier
  - Name, slug, description
  - Status (ACTIVE, SUSPENDED, TRIAL, EXPIRED)
  - Resource limits (maxUsers, maxProjects)
  - Relationships to Users and Projects

- **User.java**: User entity with authentication
  - Belongs to a Tenant
  - Role-based access (TENANT_ADMIN, PROJECT_MANAGER, TEAM_MEMBER, VIEWER)
  - Status management
  - Password hash storage
  - Relationships to Tasks

- **Project.java**: Project management entity
  - Belongs to a Tenant
  - Unique project key
  - Status tracking
  - Owner assignment
  - Relationships to Tasks

- **Task.java**: Task tracking entity
  - Belongs to a Project
  - Status and priority tracking
  - Assignment to Users
  - Time tracking
  - Due dates

### DTO Layer (`dto/`)
**Purpose**: Request/Response objects for API

- **Request DTOs**: Validation, input sanitization
- **Response DTOs**: Controlled data exposure
- **Separation**: Prevents over-exposure of domain entities

### Repository Layer (`repository/`)
**Purpose**: Data access abstraction

- Spring Data JPA interfaces
- Custom query methods
- Tenant-scoped queries
- Optimized database operations

### Service Layer (`service/`)
**Purpose**: Business logic implementation

- **Interfaces**: Define contracts
- **Implementations**: Business rules, validation
- Transaction management
- Tenant isolation enforcement

### Controller Layer (`controller/`)
**Purpose**: REST API endpoints

- Request handling
- Response formatting
- Input validation
- HTTP status codes
- API documentation ready

### Security Layer (`security/`)
**Purpose**: Authentication & Authorization

- **SecurityConfig**: Spring Security setup
- **TenantContext**: ThreadLocal tenant storage
- **TenantContextFilter**: Tenant identification
- **JwtAuthenticationFilter**: JWT validation
- **JwtService**: Token generation/validation

### Config Layer (`config/`)
**Purpose**: Application configuration

- Bean definitions
- JPA configuration
- Security beans
- Application-wide settings

### Exception Layer (`exception/`)
**Purpose**: Centralized error handling

- Global exception handler
- Standardized error responses
- Validation error formatting

## Key Features by Layer

### Entity Layer Features
- ✅ JPA annotations
- ✅ Lombok annotations (@Data, @Builder, etc.)
- ✅ Bidirectional relationships
- ✅ Cascade operations
- ✅ Automatic timestamps
- ✅ Enum types for status/roles
- ✅ Database indexes

### DTO Layer Features
- ✅ Jakarta validation annotations
- ✅ Lombok builders
- ✅ Separate request/response objects
- ✅ Field-level validation
- ✅ Custom validation messages

### Repository Layer Features
- ✅ Spring Data JPA
- ✅ Custom query methods
- ✅ @Query annotations
- ✅ Tenant-scoped queries
- ✅ Count/exists methods
- ✅ Optimized queries

### Service Layer Features
- ✅ Interface-based design
- ✅ @Transactional boundaries
- ✅ Business logic encapsulation
- ✅ Tenant validation
- ✅ Entity-to-DTO mapping
- ✅ Error handling

### Controller Layer Features
- ✅ RESTful endpoints
- ✅ @Valid input validation
- ✅ Proper HTTP status codes
- ✅ Path variables and request params
- ✅ Tenant context usage
- ✅ ResponseEntity patterns

### Security Layer Features
- ✅ JWT authentication
- ✅ Multi-tenant context
- ✅ Filter chain
- ✅ Password encryption
- ✅ Token validation
- ✅ ThreadLocal tenant storage

## File Count Summary

```
Total Java Files: 40+
├── Entities: 4
├── DTOs: 12
├── Repositories: 4
├── Services: 8 (4 interfaces + 4 implementations)
├── Controllers: 5
├── Security: 5
├── Config: 3
├── Exception: 3
└── Main Application: 1

Configuration Files: 4
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── pom.xml

Docker Files: 2
├── Dockerfile
└── docker-compose.yml

Documentation Files: 5
├── README.md
├── ARCHITECTURE.md
├── DEVELOPMENT.md
├── PROJECT_STRUCTURE.md
└── .gitignore
```

## Naming Conventions

### Classes
- **Entities**: Singular noun (User, Project, Task)
- **DTOs**: Descriptive name + Dto/Request (UserDto, CreateUserRequest)
- **Repositories**: EntityName + Repository (UserRepository)
- **Services**: EntityName + Service/ServiceImpl
- **Controllers**: EntityName + Controller

### Methods
- **Repository**: findBy, existsBy, countBy
- **Service**: create, get, getAll, update, delete
- **Controller**: HTTP verb mapping (POST, GET, PUT, DELETE)

### Packages
- Lowercase, singular form
- Organized by technical layer
- Clear separation of concerns

## Dependencies Overview

### Core Dependencies
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation

### Database
- PostgreSQL Driver
- Hibernate (via Spring Data JPA)

### Utilities
- Lombok
- JWT (jjwt-api, jjwt-impl, jjwt-jackson)

### Testing
- Spring Boot Starter Test
- Spring Security Test

## Clean Architecture Compliance

```
┌─────────────────────────────────────────────────┐
│  Controllers (Presentation)                     │
│  - REST endpoints                               │
│  - Request/Response handling                    │
└──────────────────┬──────────────────────────────┘
                   │ depends on
                   ▼
┌─────────────────────────────────────────────────┐
│  Services (Business Logic)                      │
│  - Use cases                                    │
│  - Business rules                               │
└──────────────────┬──────────────────────────────┘
                   │ depends on
                   ▼
┌─────────────────────────────────────────────────┐
│  Repositories (Data Access)                     │
│  - Database operations                          │
│  - Query methods                                │
└──────────────────┬──────────────────────────────┘
                   │ depends on
                   ▼
┌─────────────────────────────────────────────────┐
│  Entities (Domain)                              │
│  - Core business models                         │
│  - No external dependencies                     │
└─────────────────────────────────────────────────┘
```

## Next Steps for Extension

1. **Authentication Module**
   - Login/Register endpoints
   - Token refresh mechanism
   - Password reset flow

2. **Authorization**
   - Role-based access control on endpoints
   - Permission management
   - Resource-level authorization

3. **API Documentation**
   - Swagger/OpenAPI integration
   - API versioning
   - Request/Response examples

4. **Testing**
   - Unit tests for services
   - Integration tests for repositories
   - API tests for controllers

5. **Monitoring**
   - Spring Boot Actuator
   - Metrics collection
   - Health checks

6. **Caching**
   - Redis integration
   - Cache strategies
   - Cache invalidation

7. **File Upload**
   - Attachment support
   - File storage (S3, local)
   - Image processing

8. **Notifications**
   - Email service
   - Webhook support
   - Event publishing
