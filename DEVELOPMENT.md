# Development Guide

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (for local database)
- PostgreSQL 12+ (if not using Docker)
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Setting Up Development Environment

### 1. Clone the Repository

```bash
git clone <repository-url>
cd workhub
```

### 2. Start PostgreSQL with Docker

```bash
docker-compose up -d postgres
```

This will start:
- PostgreSQL on port 5432
- PgAdmin on port 5050 (optional, for database management)

Access PgAdmin at `http://localhost:5050`:
- Email: admin@workhub.com
- Password: admin

### 3. Configure Application

The default configuration in `application.yml` is set up for local development:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workhub
    username: postgres
    password: postgres
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

Or with dev profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will start on `http://localhost:8080`

## IDE Setup

### IntelliJ IDEA

1. Open the project
2. Enable annotation processing:
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"
3. Install Lombok plugin:
   - Settings → Plugins → Search "Lombok" → Install
4. Build the project: Build → Build Project

### Eclipse

1. Import as Maven project
2. Install Lombok:
   - Download lombok.jar from https://projectlombok.org/download
   - Run: `java -jar lombok.jar`
   - Select Eclipse installation directory
3. Update project: Right-click project → Maven → Update Project

### VS Code

1. Install extensions:
   - Java Extension Pack
   - Spring Boot Extension Pack
   - Lombok Annotations Support
2. Open the project folder
3. Run using Spring Boot Dashboard

## Running Tests

### All Tests
```bash
mvn test
```

### Specific Test Class
```bash
mvn test -Dtest=UserServiceTest
```

### With Coverage
```bash
mvn clean test jacoco:report
```

Coverage report will be in `target/site/jacoco/index.html`

## Database Management

### Using Docker Compose

Start database:
```bash
docker-compose up -d postgres
```

Stop database:
```bash
docker-compose down
```

Reset database (delete all data):
```bash
docker-compose down -v
docker-compose up -d postgres
```

### Manual Database Setup

If not using Docker:

```sql
-- Create database
CREATE DATABASE workhub;

-- Create user (optional)
CREATE USER workhub_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE workhub TO workhub_user;
```

### Database Migrations

The application uses Hibernate's `ddl-auto` setting:

- **dev profile**: `create-drop` (recreates schema on restart)
- **default**: `update` (updates schema automatically)
- **prod**: `validate` (only validates, no changes)

For production, consider using Flyway or Liquibase for versioned migrations.

## API Testing

### Using cURL

#### Create a Tenant
```bash
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "slug": "acme",
    "description": "Acme Corporation",
    "maxUsers": 50,
    "maxProjects": 100
  }'
```

#### Create a User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <tenant-uuid>" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@acme.com",
    "password": "SecurePass123!",
    "role": "TENANT_ADMIN"
  }'
```

#### Create a Project
```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <tenant-uuid>" \
  -d '{
    "name": "Website Redesign",
    "description": "Complete website redesign project",
    "projectKey": "WEB",
    "startDate": "2024-01-01",
    "endDate": "2024-06-30"
  }'
```

#### Create a Task
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <tenant-uuid>" \
  -d '{
    "projectId": 1,
    "title": "Design homepage mockup",
    "description": "Create initial homepage design mockup",
    "priority": "HIGH",
    "estimatedHours": 8
  }'
```

### Using Postman

1. Import the API collection (create one with above examples)
2. Set environment variables:
   - `base_url`: http://localhost:8080
   - `tenant_id`: Your tenant UUID
   - `jwt_token`: Your JWT token (after authentication)

## Debugging

### Enable Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    com.workhub: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Remote Debugging

Run with debug enabled:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

Connect your IDE debugger to port 5005.

## Code Quality

### Code Formatting

Use consistent formatting:
- 4 spaces for indentation
- Line length: 120 characters
- Follow Java naming conventions

### Static Analysis

Run SpotBugs (add to pom.xml):

```bash
mvn spotbugs:check
```

### Code Coverage

Aim for:
- Overall coverage: >80%
- Service layer: >90%
- Controller layer: >70%

## Common Development Tasks

### Add a New Entity

1. Create entity class in `entity/` package
2. Create repository interface in `repository/` package
3. Create DTOs in `dto/` package
4. Create service interface and implementation in `service/` package
5. Create controller in `controller/` package
6. Add tests for each layer

### Add a New Endpoint

1. Add method to service interface
2. Implement in service implementation
3. Add controller method with proper annotations
4. Add validation
5. Write tests
6. Update API documentation

### Modify Database Schema

1. Update entity class
2. Run application (Hibernate will update schema in dev)
3. Test changes
4. For production, create migration script

## Troubleshooting

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Database Connection Issues

1. Check if PostgreSQL is running:
   ```bash
   docker-compose ps
   ```

2. Check connection settings in `application.yml`

3. Test connection:
   ```bash
   psql -h localhost -U postgres -d workhub
   ```

### Lombok Not Working

1. Verify Lombok plugin is installed
2. Enable annotation processing in IDE
3. Rebuild project
4. Restart IDE

### Build Failures

```bash
# Clean and rebuild
mvn clean install -U

# Skip tests if needed
mvn clean install -DskipTests
```

## Performance Optimization

### Database Query Optimization

1. Enable SQL logging:
   ```yaml
   spring:
     jpa:
       show-sql: true
       properties:
         hibernate:
           format_sql: true
   ```

2. Check for N+1 queries
3. Use appropriate fetch strategies
4. Add indexes for frequently queried columns

### Application Performance

1. Use profiler (JProfiler, YourKit, VisualVM)
2. Monitor memory usage
3. Check for memory leaks
4. Optimize service layer logic

## Git Workflow

### Branch Naming

- Feature: `feature/description`
- Bug fix: `bugfix/description`
- Hotfix: `hotfix/description`

### Commit Messages

Follow conventional commits:

```
feat: add user authentication
fix: resolve tenant isolation bug
docs: update API documentation
test: add unit tests for ProjectService
refactor: simplify UserService logic
```

### Before Committing

1. Run tests: `mvn test`
2. Check formatting
3. Update documentation if needed
4. Review changes

## Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [Lombok](https://projectlombok.org/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
