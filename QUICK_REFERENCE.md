# Spring Boot Manual Setup - Quick Reference

## 🚀 Quick Start (5 Commands)

```bash
# 1. Start database
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15-alpine

# 2. Navigate to project
cd manual-setup-example

# 3. Build
mvn clean install

# 4. Run
mvn spring-boot:run

# 5. Test
curl http://localhost:8080/api/hello
```

## 📦 Essential Dependencies

```xml
<!-- REST API -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Utilities -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## 🏗️ Project Structure

```
src/main/java/com/example/app/
├── Application.java          # @SpringBootApplication
├── controller/               # @RestController
├── service/                  # @Service + @Transactional
├── repository/               # extends JpaRepository
└── model/                    # @Entity
```

## 🎯 Key Annotations

### Class Level
```java
@SpringBootApplication        // Main class
@RestController              // REST endpoints
@Service                     // Business logic
@Repository                  // Data access
@Entity                      // JPA entity
@Configuration               // Configuration class
```

### Method Level
```java
@GetMapping("/path")         // GET request
@PostMapping("/path")        // POST request
@PutMapping("/path")         // PUT request
@DeleteMapping("/path")      // DELETE request
@Transactional               // Transaction boundary
```

### Parameter Level
```java
@PathVariable                // From URL path
@RequestParam                // From query string
@RequestBody                 // From request body
@Valid                       // Trigger validation
```

### Field Level
```java
@Id                          // Primary key
@GeneratedValue              // Auto-generate
@Column                      // Column mapping
@NotNull, @NotBlank          // Validation
@Email, @Size                // Validation
```

### Lombok
```java
@Data                        // Getters, setters, toString, equals, hashCode
@Builder                     // Builder pattern
@NoArgsConstructor          // No-arg constructor
@AllArgsConstructor         // All-args constructor
@RequiredArgsConstructor    // Constructor for final fields
@Slf4j                      // Logger
```

## 🔧 Essential Configuration

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/dbname
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Server
server.port=8080

# Logging
logging.level.root=INFO
logging.level.com.example=DEBUG
```

## 🌐 REST API Patterns

### Create
```java
@PostMapping
public ResponseEntity<User> create(@Valid @RequestBody User user) {
    User created = service.create(user);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
}
```

### Read
```java
@GetMapping("/{id}")
public ResponseEntity<User> getById(@PathVariable Long id) {
    User user = service.getById(id);
    return ResponseEntity.ok(user);
}
```

### Update
```java
@PutMapping("/{id}")
public ResponseEntity<User> update(@PathVariable Long id, 
                                   @Valid @RequestBody User user) {
    User updated = service.update(id, user);
    return ResponseEntity.ok(updated);
}
```

### Delete
```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
}
```

## 💾 JPA Entity Template

```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    @NotBlank
    private String name;
    
    @Column(unique = true)
    @Email
    private String email;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

## 🗄️ Repository Template

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByNameContaining(String name);
    boolean existsByEmail(String email);
}
```

## 💼 Service Template

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    private final UserRepository repository;
    
    public User create(User user) {
        log.info("Creating user: {}", user.getEmail());
        return repository.save(user);
    }
    
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
```

## 🧪 Testing Endpoints

```bash
# Create
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@test.com"}'

# Read
curl http://localhost:8080/api/users/1

# Update
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"John Updated","email":"john@test.com"}'

# Delete
curl -X DELETE http://localhost:8080/api/users/1

# List
curl http://localhost:8080/api/users
```

## 📊 HTTP Status Codes

```
200 OK              - Success (GET, PUT)
201 Created         - Resource created (POST)
204 No Content      - Success, no body (DELETE)
400 Bad Request     - Invalid input
404 Not Found       - Resource not found
500 Server Error    - Server error
```

## 🔍 Common Query Methods

```java
// Exact match
findByName(String name)
findByEmail(String email)

// Comparison
findByAgeGreaterThan(Integer age)
findByAgeLessThan(Integer age)
findByAgeBetween(Integer min, Integer max)

// Pattern matching
findByNameContaining(String name)
findByNameStartingWith(String prefix)
findByNameEndingWith(String suffix)

// Boolean
findByActiveTrue()
findByActiveFalse()

// Null checks
findByEmailIsNull()
findByEmailIsNotNull()

// Multiple conditions
findByNameAndEmail(String name, String email)
findByNameOrEmail(String name, String email)

// Sorting
findAllByOrderByNameAsc()
findAllByOrderByCreatedAtDesc()

// Existence
existsByEmail(String email)

// Counting
countByActive(Boolean active)
```

## 🛠️ Maven Commands

```bash
# Clean and build
mvn clean install

# Run application
mvn spring-boot:run

# Run tests
mvn test

# Package as JAR
mvn package

# Skip tests
mvn clean install -DskipTests

# Dependency tree
mvn dependency:tree

# Run specific test
mvn test -Dtest=UserServiceTest
```

## 🐛 Troubleshooting

```bash
# Port in use
# Change in application.properties:
server.port=8081

# Database connection failed
# Check PostgreSQL is running:
docker ps

# Lombok not working
# 1. Install Lombok plugin
# 2. Enable annotation processing
# 3. Restart IDE

# Build fails
mvn clean install -U
```

## 📁 File Locations

```
Project Root
├── src/main/java/          # Java source code
├── src/main/resources/     # Configuration files
├── src/test/java/          # Test code
├── pom.xml                 # Maven configuration
└── target/                 # Build output
```

## 🎯 What Each Layer Does

```
Controller  → Handle HTTP requests/responses
Service     → Business logic + transactions
Repository  → Database operations
Entity      → Data model
DTO         → Data transfer objects
Config      → Configuration classes
```

## 📚 Documentation Files

```
MANUAL_SETUP_GUIDE.md       # Complete setup guide
MANUAL_SETUP_COMPLETE.md    # What was created
manual-setup-example/README.md  # Project README
QUICK_REFERENCE.md          # This file
```

## 🎓 Learning Order

```
1. Application.java         # Startup
2. HelloController.java     # Simple REST
3. User.java               # JPA Entity
4. UserRepository.java     # Data Access
5. UserService.java        # Business Logic
6. UserController.java     # Full CRUD
7. application.properties  # Configuration
8. pom.xml                # Dependencies
```

## ⚡ Pro Tips

1. **Use Lombok** - Reduces code by 70%
2. **@Transactional** - Always on service layer
3. **@Valid** - Validate input automatically
4. **readOnly = true** - Optimize read operations
5. **ResponseEntity** - Full control over responses
6. **Optional** - Handle null safely
7. **@Slf4j** - Easy logging
8. **Builder pattern** - Clean object creation

## 🚀 Production Checklist

```
□ Change ddl-auto to 'validate'
□ Disable SQL logging
□ Use environment variables for secrets
□ Enable actuator endpoints
□ Configure proper logging
□ Set up monitoring
□ Use connection pooling
□ Add error handling
□ Implement security
□ Add API documentation
```

---

**Quick Reference Complete!** 📖

For detailed explanations, see the full documentation files.
