# Manual Spring Boot Setup Example

## Overview

This project demonstrates a **completely manual** Spring Boot setup without relying on auto-generated starters or hidden configurations. Every dependency, configuration, and component is explicitly defined and thoroughly documented.

## What Makes This "Manual"?

### ✅ What We Do
- **Explicit dependency declaration** in `pom.xml`
- **Manual configuration** in `application.properties`
- **Clear understanding** of what each component does
- **No hidden magic** - everything is documented
- **Full control** over the application setup

### ❌ What We Avoid
- Auto-generated project templates
- Hidden auto-configuration (we understand what's happening)
- Unnecessary dependencies
- Black-box behavior

## Project Structure

```
manual-setup-example/
├── src/
│   ├── main/
│   │   ├── java/com/example/app/
│   │   │   ├── Application.java              # Main entry point
│   │   │   ├── controller/
│   │   │   │   ├── HelloController.java      # Simple REST examples
│   │   │   │   └── UserController.java       # Full CRUD REST API
│   │   │   ├── model/
│   │   │   │   └── User.java                 # JPA Entity with Lombok
│   │   │   ├── repository/
│   │   │   │   └── UserRepository.java       # Spring Data JPA
│   │   │   └── service/
│   │   │       └── UserService.java          # Business logic
│   │   └── resources/
│   │       └── application.properties        # Configuration
│   └── test/
│       └── java/com/example/app/
│           └── ApplicationTests.java
├── pom.xml                                    # Maven dependencies
└── README.md                                  # This file
```

## Dependencies Explained

### 1. spring-boot-starter-web
**What it provides:**
- Spring MVC for REST APIs
- Embedded Tomcat server
- Jackson for JSON serialization

**Why we need it:**
- Build REST controllers
- Handle HTTP requests/responses

**Transitive dependencies:**
- `spring-webmvc`
- `spring-web`
- `tomcat-embed-core`
- `jackson-databind`

### 2. spring-boot-starter-data-jpa
**What it provides:**
- Spring Data JPA repositories
- Hibernate as JPA implementation
- Transaction management

**Why we need it:**
- Database operations with JPA
- Automatic repository implementation

**Transitive dependencies:**
- `hibernate-core`
- `spring-data-jpa`
- `spring-orm`
- `HikariCP` (connection pooling)

### 3. postgresql
**What it provides:**
- JDBC driver for PostgreSQL

**Why we need it:**
- Connect to PostgreSQL database
- Execute SQL queries

**Scope:** `runtime` (not needed during compilation)

### 4. lombok
**What it provides:**
- Annotation processor for code generation
- Reduces boilerplate code

**Why we need it:**
- Auto-generate getters, setters, constructors
- Cleaner entity and DTO classes

**Annotations used:**
- `@Data`, `@Getter`, `@Setter`
- `@NoArgsConstructor`, `@AllArgsConstructor`
- `@Builder`, `@RequiredArgsConstructor`
- `@Slf4j`

### 5. spring-boot-starter-validation
**What it provides:**
- Bean Validation (JSR-380)
- Hibernate Validator

**Why we need it:**
- Validate request data
- Ensure data integrity

**Annotations used:**
- `@NotNull`, `@NotBlank`, `@Email`
- `@Size`, `@Min`, `@Max`
- `@Valid`

### 6. spring-boot-starter-test
**What it provides:**
- JUnit 5 testing framework
- Mockito for mocking
- Spring Test support

**Why we need it:**
- Write unit and integration tests

**Scope:** `test` (not included in final JAR)

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (or use H2 for in-memory database)

### Step 1: Start Database

Using Docker:
```bash
docker run --name postgres-manual \
  -e POSTGRES_DB=workhub \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:15-alpine
```

Or use the docker-compose.yml from the parent project.

### Step 2: Build Project
```bash
cd manual-setup-example
mvn clean install
```

### Step 3: Run Application
```bash
mvn spring-boot:run
```

Application starts at: `http://localhost:8080`

## Testing the API

### 1. Health Check
```bash
curl http://localhost:8080/api/hello
```

Expected: `"Hello from manually configured Spring Boot!"`

### 2. Create a User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "age": 30,
    "active": true
  }'
```

### 3. Get All Users
```bash
curl http://localhost:8080/api/users
```

### 4. Get User by ID
```bash
curl http://localhost:8080/api/users/1
```

### 5. Update User
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Updated",
    "email": "john.updated@example.com",
    "age": 31,
    "active": true
  }'
```

### 6. Search Users
```bash
curl "http://localhost:8080/api/users/search?name=John"
```

### 7. Delete User
```bash
curl -X DELETE http://localhost:8080/api/users/1
```

### 8. Application Status
```bash
curl http://localhost:8080/api/status
```

## What's Happening Under the Hood

### Application Startup Sequence

1. **JVM starts** - Runs `Application.main()`
2. **SpringApplication.run()** - Initializes Spring context
3. **Component scanning** - Finds `@Component`, `@Service`, `@Repository`, `@Controller`
4. **Auto-configuration** - Based on classpath dependencies
5. **Bean creation** - Creates and wires beans
6. **Tomcat starts** - Embedded server starts on port 8080
7. **Application ready** - Ready to handle requests

### Request Flow

```
HTTP Request
    ↓
DispatcherServlet (Spring MVC)
    ↓
Controller (@RestController)
    ↓
Service (@Service + @Transactional)
    ↓
Repository (Spring Data JPA)
    ↓
Hibernate (JPA Implementation)
    ↓
HikariCP (Connection Pool)
    ↓
PostgreSQL Database
    ↓
Response flows back up
    ↓
JSON serialization (Jackson)
    ↓
HTTP Response
```

### Transaction Flow

```
@Transactional method called
    ↓
Spring opens database transaction
    ↓
Method executes
    ↓
If success: COMMIT
If exception: ROLLBACK
    ↓
Transaction closed
```

## Key Concepts Demonstrated

### 1. Dependency Injection
```java
@RequiredArgsConstructor  // Lombok generates constructor
private final UserService userService;  // Injected by Spring
```

### 2. Repository Pattern
```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring generates implementation automatically
    Optional<User> findByEmail(String email);
}
```

### 3. Transaction Management
```java
@Transactional  // Automatic commit/rollback
public User createUser(User user) {
    return userRepository.save(user);
}
```

### 4. Bean Validation
```java
@PostMapping
public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
    // @Valid triggers validation
    // Returns 400 if validation fails
}
```

### 5. Exception Handling
```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<String> handleException(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
}
```

## Configuration Files

### application.properties
Contains all explicit configurations:
- Database connection
- JPA/Hibernate settings
- Logging levels
- Server port
- Jackson JSON settings

Every property is documented with:
- What it does
- Why we need it
- Alternatives
- Production considerations

## What's NOT Auto-Configured

While Spring Boot has auto-configuration, we've made everything explicit:

1. **Database connection** - Manually configured in `application.properties`
2. **JPA settings** - Explicitly set DDL mode, SQL logging, etc.
3. **Logging** - Configured log levels for each package
4. **Server settings** - Port, error handling, etc.
5. **Jackson** - Date format, time zone, etc.

## Dependency Tree

View the complete dependency tree:
```bash
mvn dependency:tree
```

This shows all transitive dependencies and where they come from.

## Building for Production

### Create executable JAR
```bash
mvn clean package
```

### Run the JAR
```bash
java -jar target/manual-spring-boot-1.0.0.jar
```

### With production profile
```bash
java -jar target/manual-spring-boot-1.0.0.jar --spring.profiles.active=prod
```

## Alternative: Using H2 Database

For testing without PostgreSQL, use H2 in-memory database:

1. Add H2 dependency to `pom.xml` (already commented out)
2. Update `application.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

3. Access H2 Console: `http://localhost:8080/h2-console`

## Learning Resources

Each Java file contains extensive documentation explaining:
- What each annotation does
- Why we use it
- What Spring does behind the scenes
- Alternative approaches
- Best practices

Start with:
1. `Application.java` - Application startup
2. `HelloController.java` - Simple REST examples
3. `User.java` - JPA entity with Lombok
4. `UserRepository.java` - Spring Data JPA
5. `UserService.java` - Business logic and transactions
6. `UserController.java` - Full CRUD REST API

## Comparison: Auto vs Manual

### Auto-Generated (Spring Initializr)
- ✅ Fast to start
- ❌ Hidden configurations
- ❌ Less understanding
- ❌ Harder to customize

### Manual Setup (This Project)
- ✅ Full understanding
- ✅ Complete control
- ✅ Easy to customize
- ✅ Better learning
- ❌ Takes more time initially

## Next Steps

1. Add authentication (Spring Security)
2. Add more entities and relationships
3. Implement pagination and sorting
4. Add integration tests
5. Add API documentation (Swagger)
6. Implement caching
7. Add file upload
8. Deploy to production

## Troubleshooting

### Port 8080 already in use
Change in `application.properties`:
```properties
server.port=8081
```

### Database connection failed
1. Check PostgreSQL is running
2. Verify credentials in `application.properties`
3. Check database exists: `CREATE DATABASE workhub;`

### Lombok not working
1. Install Lombok plugin in IDE
2. Enable annotation processing
3. Restart IDE

## Summary

This project demonstrates that Spring Boot is **not magic** - it's a well-designed framework with:
- Clear dependency management
- Sensible defaults
- Explicit configuration options
- Powerful auto-configuration (that we understand)

By setting everything up manually, you gain:
- Deep understanding of Spring Boot
- Ability to customize anything
- Confidence in debugging issues
- Knowledge to make informed decisions

**The goal**: Understand what's happening, not just make it work.
