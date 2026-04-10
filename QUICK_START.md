# Quick Start Guide

## 🚀 Get Running in 5 Minutes

### Prerequisites Check
```bash
java -version    # Should be 17+
mvn -version     # Should be 3.6+
docker --version # For database
```

### Step 1: Start Database (30 seconds)
```bash
docker-compose up -d postgres
```

Wait for PostgreSQL to be ready:
```bash
docker-compose logs -f postgres
# Wait for "database system is ready to accept connections"
```

### Step 2: Build Project (1 minute)
```bash
mvn clean install
```

### Step 3: Run Application (30 seconds)
```bash
mvn spring-boot:run
```

Application starts at: `http://localhost:8080`

### Step 4: Test It Works (1 minute)

#### Health Check
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP"
}
```

#### Create Your First Tenant
```bash
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Company",
    "slug": "mycompany",
    "description": "My awesome company",
    "maxUsers": 100,
    "maxProjects": 50
  }'
```

Expected response (save the `id`):
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "My Company",
  "slug": "mycompany",
  ...
}
```

#### Create Your First User
```bash
# Replace <TENANT_ID> with the id from above
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <TENANT_ID>" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@mycompany.com",
    "password": "SecurePass123!",
    "role": "TENANT_ADMIN"
  }'
```

#### Create Your First Project
```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <TENANT_ID>" \
  -d '{
    "name": "Website Redesign",
    "description": "Redesign company website",
    "projectKey": "WEB",
    "startDate": "2024-01-01"
  }'
```

#### Create Your First Task
```bash
# Replace <PROJECT_ID> with the project id from above
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <TENANT_ID>" \
  -d '{
    "projectId": <PROJECT_ID>,
    "title": "Design homepage mockup",
    "description": "Create initial design",
    "priority": "HIGH",
    "estimatedHours": 8
  }'
```

### Step 5: View Your Data

#### Get All Projects
```bash
curl http://localhost:8080/api/projects \
  -H "X-Tenant-ID: <TENANT_ID>"
```

#### Get All Tasks
```bash
curl http://localhost:8080/api/tasks \
  -H "X-Tenant-ID: <TENANT_ID>"
```

#### Get All Users
```bash
curl http://localhost:8080/api/users \
  -H "X-Tenant-ID: <TENANT_ID>"
```

## 🎯 Common Operations

### Update a Task Status
```bash
curl -X PUT http://localhost:8080/api/tasks/<TASK_ID> \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <TENANT_ID>" \
  -d '{
    "status": "IN_PROGRESS"
  }'
```

### Assign a Task to a User
```bash
curl -X PUT http://localhost:8080/api/tasks/<TASK_ID> \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <TENANT_ID>" \
  -d '{
    "assignedToId": <USER_ID>
  }'
```

### Get Tasks by Status
```bash
curl "http://localhost:8080/api/tasks/project/<PROJECT_ID>/status/TODO" \
  -H "X-Tenant-ID: <TENANT_ID>"
```

### Get Overdue Tasks
```bash
curl http://localhost:8080/api/tasks/overdue \
  -H "X-Tenant-ID: <TENANT_ID>"
```

## 🛠️ Development Tools

### Access Database with PgAdmin
1. Start PgAdmin:
   ```bash
   docker-compose up -d pgadmin
   ```

2. Open browser: `http://localhost:5050`

3. Login:
   - Email: `admin@workhub.com`
   - Password: `admin`

4. Add Server:
   - Host: `postgres` (container name)
   - Port: `5432`
   - Database: `workhub`
   - Username: `postgres`
   - Password: `postgres`

### View Logs
```bash
# Application logs (in terminal where mvn spring-boot:run is running)

# Database logs
docker-compose logs -f postgres
```

### Stop Everything
```bash
# Stop application: Ctrl+C in terminal

# Stop database
docker-compose down

# Stop and remove data
docker-compose down -v
```

## 📊 API Endpoints Quick Reference

### Tenants
- `POST /api/tenants` - Create
- `GET /api/tenants/{id}` - Get by ID
- `GET /api/tenants/slug/{slug}` - Get by slug
- `PUT /api/tenants/{id}` - Update
- `DELETE /api/tenants/{id}` - Delete

### Users (require X-Tenant-ID header)
- `POST /api/users` - Create
- `GET /api/users` - List all
- `GET /api/users/{id}` - Get by ID
- `GET /api/users/email/{email}` - Get by email
- `PUT /api/users/{id}` - Update
- `DELETE /api/users/{id}` - Delete

### Projects (require X-Tenant-ID header)
- `POST /api/projects` - Create
- `GET /api/projects` - List all
- `GET /api/projects/{id}` - Get by ID
- `GET /api/projects/key/{key}` - Get by key
- `PUT /api/projects/{id}` - Update
- `DELETE /api/projects/{id}` - Delete

### Tasks (require X-Tenant-ID header)
- `POST /api/tasks` - Create
- `GET /api/tasks` - List all
- `GET /api/tasks/{id}` - Get by ID
- `GET /api/tasks/project/{projectId}` - By project
- `GET /api/tasks/assignee/{userId}` - By assignee
- `GET /api/tasks/overdue` - Overdue tasks
- `PUT /api/tasks/{id}` - Update
- `DELETE /api/tasks/{id}` - Delete

## 🔧 Configuration

### Change Database Settings
Edit `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workhub
    username: your_username
    password: your_password
```

### Change Server Port
Edit `src/main/resources/application.yml`:
```yaml
server:
  port: 8080  # Change to your preferred port
```

### Enable/Disable SQL Logging
Edit `src/main/resources/application.yml`:
```yaml
spring:
  jpa:
    show-sql: true  # Set to false to disable
```

## 🐛 Troubleshooting

### Port 8080 Already in Use
```bash
# Find process
netstat -ano | findstr :8080

# Kill process (Windows)
taskkill /PID <PID> /F

# Or change port in application.yml
```

### Database Connection Failed
```bash
# Check if PostgreSQL is running
docker-compose ps

# Restart database
docker-compose restart postgres

# Check logs
docker-compose logs postgres
```

### Application Won't Start
```bash
# Clean and rebuild
mvn clean install -U

# Check Java version
java -version  # Must be 17+

# Check Maven version
mvn -version   # Must be 3.6+
```

### Lombok Not Working
1. Enable annotation processing in your IDE
2. Install Lombok plugin
3. Restart IDE
4. Rebuild project

## 📚 Next Steps

1. **Read the Documentation**
   - `README.md` - Complete overview
   - `ARCHITECTURE.md` - Architecture details
   - `DEVELOPMENT.md` - Development guide

2. **Add Authentication**
   - Implement login/register endpoints
   - Add JWT token generation
   - Secure endpoints with authentication

3. **Add Tests**
   - Unit tests for services
   - Integration tests for repositories
   - API tests for controllers

4. **Add Frontend**
   - React, Vue, or Angular
   - Connect to REST API
   - Handle JWT tokens

5. **Deploy to Production**
   - Set up production database
   - Configure environment variables
   - Deploy with Docker

## 💡 Tips

- Always include `X-Tenant-ID` header for tenant-scoped operations
- Use meaningful project keys (2-5 uppercase letters)
- Set realistic estimated hours for tasks
- Use appropriate task priorities
- Keep project and task descriptions clear

## 🎓 Learning Resources

- Spring Boot: https://spring.io/projects/spring-boot
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Spring Security: https://spring.io/projects/spring-security
- PostgreSQL: https://www.postgresql.org/docs/
- Docker: https://docs.docker.com/

## 📞 Need Help?

1. Check the logs for error messages
2. Review the documentation files
3. Check the troubleshooting section
4. Verify your configuration
5. Ensure all prerequisites are met

---

**You're all set! 🎉**

Your multi-tenant SaaS backend is running and ready for development!
