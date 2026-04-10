# WorkHub - Multi-Tenant SaaS Backend

A comprehensive Spring Boot backend application for a multi-tenant SaaS platform with project and task management capabilities.

## Architecture

This project follows **Clean Architecture** principles with clear separation of concerns:

```
src/main/java/com/workhub/
├── entity/              # Domain entities (JPA entities)
│   ├── Tenant.java
│   ├── User.java
│   ├── Project.java
│   └── Task.java
├── dto/                 # Data Transfer Objects
│   ├── TenantResponse.java
│   ├── CreateTenantRequest.java
│   ├── UpdateTenantRequest.java
│   ├── UserDto.java
│   ├── CreateUserRequest.java
│   ├── UpdateUserRequest.java
│   ├── ProjectDto.java
│   ├── CreateProjectRequest.java
│   ├── UpdateProjectRequest.java
│   ├── TaskDto.java
│   ├── CreateTaskRequest.java
│   └── UpdateTaskRequest.java
├── repository/          # Data access layer (Spring Data JPA)
│   ├── TenantRepository.java
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   └── TaskRepository.java
├── service/             # Business logic layer
│   ├── TenantService.java
│   ├── TenantServiceImpl.java
│   ├── UserService.java
│   ├── UserServiceImpl.java
│   ├── ProjectService.java
│   ├── ProjectServiceImpl.java
│   ├── TaskService.java
│   └── TaskServiceImpl.java
├── controller/          # REST API endpoints
│   ├── HealthController.java
│   ├── TenantController.java
│   ├── UserController.java
│   ├── ProjectController.java
│   └── TaskController.java
├── security/            # Security & authentication
│   ├── SecurityConfig.java
│   ├── TenantContext.java
│   ├── TenantContextFilter.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtService.java
├── config/              # Application configuration
│   ├── ApplicationConfig.java
│   ├── JpaConfig.java
│   └── PasswordEncoderConfig.java
├── exception/           # Exception handling
│   ├── GlobalExceptionHandler.java
│   ├── ErrorResponse.java
│   └── ValidationErrorResponse.java
└── WorkHubApplication.java
```

## Features

### Multi-Tenancy
- **Tenant Isolation**: Each tenant's data is completely isolated
- **Tenant Context**: Automatic tenant identification via headers
- **Tenant Validation**: All operations validate tenant access

### Entities

#### Tenant
- Unique identifier (UUID)
- Name and slug
- Status (ACTIVE, SUSPENDED, TRIAL, EXPIRED)
- Resource limits (max users, max projects)
- Timestamps

#### User
- Belongs to a tenant
- Role-based access (TENANT_ADMIN, PROJECT_MANAGER, TEAM_MEMBER, VIEWER)
- Status management (ACTIVE, INACTIVE, SUSPENDED)
- Password encryption
- Last login tracking

#### Project
- Belongs to a tenant
- Unique project key
- Status tracking (ACTIVE, ON_HOLD, COMPLETED, ARCHIVED)
- Owner assignment
- Start and end dates
- Task relationships

#### Task
- Belongs to a project
- Status tracking (TODO, IN_PROGRESS, IN_REVIEW, COMPLETED, BLOCKED)
- Priority levels (LOW, MEDIUM, HIGH, URGENT)
- Assignment to users
- Time tracking (estimated vs actual hours)
- Due dates
- Completion tracking

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.3.5
- **Spring Data JPA**: Database operations
- **Spring Security**: Authentication & authorization
- **PostgreSQL**: Primary database
- **Lombok**: Reduce boilerplate code
- **JWT**: Token-based authentication
- **Maven**: Build tool

## API Endpoints

### Tenant Management
- `POST /api/tenants` - Create tenant
- `GET /api/tenants/{id}` - Get tenant by ID
- `GET /api/tenants/slug/{slug}` - Get tenant by slug
- `PUT /api/tenants/{id}` - Update tenant
- `DELETE /api/tenants/{id}` - Delete tenant

### User Management
- `POST /api/users` - Create user
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/email/{email}` - Get user by email
- `GET /api/users` - Get all users (tenant-scoped)
- `GET /api/users/status/{status}` - Get users by status
- `GET /api/users/role/{role}` - Get users by role
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- `GET /api/users/count` - Count users

### Project Management
- `POST /api/projects` - Create project
- `GET /api/projects/{id}` - Get project by ID
- `GET /api/projects/key/{projectKey}` - Get project by key
- `GET /api/projects` - Get all projects (tenant-scoped)
- `GET /api/projects/status/{status}` - Get projects by status
- `GET /api/projects/owner/{ownerId}` - Get projects by owner
- `PUT /api/projects/{id}` - Update project
- `DELETE /api/projects/{id}` - Delete project
- `GET /api/projects/count` - Count projects

### Task Management
- `POST /api/tasks` - Create task
- `GET /api/tasks/{id}` - Get task by ID
- `GET /api/tasks` - Get all tasks (tenant-scoped)
- `GET /api/tasks/project/{projectId}` - Get tasks by project
- `GET /api/tasks/assignee/{userId}` - Get tasks by assignee
- `GET /api/tasks/project/{projectId}/status/{status}` - Get tasks by project and status
- `GET /api/tasks/assignee/{userId}/status/{status}` - Get tasks by assignee and status
- `GET /api/tasks/overdue` - Get overdue tasks
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task
- `GET /api/tasks/project/{projectId}/count` - Count tasks by project

## Getting Started

### Prerequisites
- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher

### Database Setup

1. Create a PostgreSQL database:
```sql
CREATE DATABASE workhub;
```

2. Update database credentials in `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workhub
    username: your_username
    password: your_password
```

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

Or run with a specific profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will start on `http://localhost:8080`

### Running Tests
```bash
mvn test
```

## Configuration

### Application Profiles

- **default**: Base configuration
- **dev**: Development environment (auto-creates schema)
- **prod**: Production environment (validates schema only)

### Environment Variables

For production deployment, set these environment variables:

- `DATABASE_URL`: PostgreSQL connection URL
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password
- `JWT_SECRET`: Secret key for JWT token generation

## Security

### Multi-Tenant Security
- Tenant context is established via `X-Tenant-ID` header
- All API calls are scoped to the tenant context
- Cross-tenant data access is prevented

### Authentication
- JWT-based authentication
- Password encryption using BCrypt
- Token expiration: 24 hours (configurable)

### Authorization
- Role-based access control
- Four user roles: TENANT_ADMIN, PROJECT_MANAGER, TEAM_MEMBER, VIEWER

## Database Schema

The application uses JPA/Hibernate to automatically manage the database schema. Key features:

- **Indexes**: Optimized queries on frequently accessed columns
- **Relationships**: Proper foreign key constraints
- **Cascading**: Automatic cleanup of related entities
- **Timestamps**: Automatic creation and update timestamps

## Best Practices Implemented

1. **Clean Architecture**: Clear separation between layers
2. **DTO Pattern**: Separate request/response objects from entities
3. **Service Layer**: Business logic isolated from controllers
4. **Repository Pattern**: Data access abstraction
5. **Exception Handling**: Global exception handler with proper error responses
6. **Validation**: Input validation using Bean Validation
7. **Lombok**: Reduced boilerplate code
8. **Transaction Management**: Proper transaction boundaries
9. **Logging**: Structured logging with SLF4J
10. **Configuration Management**: Profile-based configuration

## Future Enhancements

- [ ] Add authentication endpoints (login, register, refresh token)
- [ ] Implement role-based authorization on endpoints
- [ ] Add pagination and sorting for list endpoints
- [ ] Implement search and filtering capabilities
- [ ] Add audit logging for sensitive operations
- [ ] Implement rate limiting
- [ ] Add API documentation with Swagger/OpenAPI
- [ ] Add integration tests
- [ ] Implement caching with Redis
- [ ] Add email notifications
- [ ] Implement file upload for attachments
- [ ] Add webhooks for external integrations

## License

This project is licensed under the MIT License.

## Contact

For questions or support, please contact the development team.
