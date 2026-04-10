# Manual Spring Boot Project Setup Guide

## Overview

This guide demonstrates how to set up a Spring Boot project **manually** without relying on auto-generated starters or hidden configurations. Every dependency and configuration is explicitly defined and explained.

## Project Information

- **Java Version**: 17
- **Spring Boot Version**: 3.3.5
- **Build Tool**: Maven
- **Packaging**: JAR

## Step 1: Create Project Structure

### Directory Structure

```
my-spring-boot-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── app/
│   │   │               ├── Application.java
│   │   │               ├── controller/
│   │   │               │   └── HelloController.java
│   │   │               ├── model/
│   │   │               │   └── User.java
│   │   │               ├── repository/
│   │   │               │   └── UserRepository.java
│   │   │               ├── service/
│   │   │               │   └── UserService.java
│   │   │               └── config/
│   │   │                   └── AppConfig.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application.yml (alternative)
│   └── test/
│       └── java/
│           └── com/
│               └── example/
│                   └── app/
│                       └── ApplicationTests.java
├── pom.xml
└── README.md
```

## Step 2: Create pom.xml (Manual Dependency Management)

### Understanding Each Dependency

#### 1. **Spring Boot Parent POM**
- Provides dependency management
- Sets Java version
- Configures Maven plugins
- **NOT** a starter - just dependency versions

#### 2. **spring-boot-starter-web**
- Includes: Spring MVC, Tomcat (embedded server), Jackson (JSON)
- Purpose: Build REST APIs and web applications
- Transitive dependencies are explicit

#### 3. **spring-boot-starter-data-jpa**
- Includes: Hibernate, Spring Data JPA, JDBC
- Purpose: Database operations with JPA
- Requires a database driver

#### 4. **Database Driver (PostgreSQL)**
- Direct JDBC driver
- No auto-configuration magic
- Must be explicitly configured

#### 5. **Lombok**
- Compile-time annotation processor
- Reduces boilerplate code
- Optional but recommended

#### 6. **spring-boot-starter-validation**
- Bean Validation (JSR-380)
- Hibernate Validator implementation
- For input validation

#### 7. **spring-boot-starter-test**
- JUnit 5, Mockito, AssertJ
- Spring Test support
- Only for testing

### Complete pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Parent POM - Only for dependency version management -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <!-- Project Coordinates -->
    <groupId>com.example</groupId>
    <artifactId>my-spring-boot-app</artifactId>
    <version>1.0.0</version>
    <name>My Spring Boot Application</name>
    <description>Manual Spring Boot setup without auto-generation</description>

    <!-- Java Version -->
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- 
            DEPENDENCY 1: Spring Boot Starter Web
            - Provides Spring MVC for REST APIs
            - Includes embedded Tomcat server
            - Includes Jackson for JSON serialization
            - Version managed by parent POM
        -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <!-- No version needed - inherited from parent -->
        </dependency>

        <!-- 
            DEPENDENCY 2: Spring Boot Starter Data JPA
            - Provides Spring Data JPA repositories
            - Includes Hibernate as JPA implementation
            - Includes Spring JDBC
            - Requires a database driver
        -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- 
            DEPENDENCY 3: PostgreSQL JDBC Driver
            - Direct database driver
            - No auto-configuration - must configure manually
            - Runtime scope - not needed for compilation
        -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- 
            DEPENDENCY 4: H2 Database (Alternative for development)
            - In-memory database for testing/development
            - Can be used instead of PostgreSQL
            - Uncomment to use
        -->
        <!--
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        -->

        <!-- 
            DEPENDENCY 5: Lombok
            - Annotation processor for reducing boilerplate
            - Generates getters, setters, constructors at compile time
            - Optional - set to true means not transitive
        -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 
            DEPENDENCY 6: Bean Validation
            - JSR-380 Bean Validation API
            - Hibernate Validator implementation
            - For @Valid, @NotNull, etc.
        -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- 
            DEPENDENCY 7: Spring Boot DevTools (Optional)
            - Automatic restart on code changes
            - LiveReload support
            - Only for development
        -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- 
            DEPENDENCY 8: Spring Boot Actuator (Optional)
            - Production-ready features
            - Health checks, metrics, info endpoints
            - Monitoring and management
        -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- 
            DEPENDENCY 9: Spring Boot Test
            - JUnit 5 (Jupiter)
            - Mockito for mocking
            - AssertJ for assertions
            - Spring Test support
            - Test scope only
        -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- 
                PLUGIN 1: Spring Boot Maven Plugin
                - Packages application as executable JAR
                - Includes all dependencies
                - Configures main class
                - Enables 'mvn spring-boot:run'
            -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <!-- Exclude Lombok from final JAR -->
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- 
                PLUGIN 2: Maven Compiler Plugin
                - Explicitly set Java version
                - Configure annotation processing
            -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                    <annotationProcessorPaths>
                        <!-- Enable Lombok annotation processing -->
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## Step 3: Alternative - Gradle Build (build.gradle)

If you prefer Gradle over Maven:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '1.0.0'
sourceCompatibility = '17'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Web - REST API support
    implementation 'org.springframework.boot:spring-boot-starter-web'
    
    // Spring Data JPA - Database access
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    
    // PostgreSQL Driver
    runtimeOnly 'org.postgresql:postgresql'
    
    // Lombok - Reduce boilerplate
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Bean Validation
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    
    // DevTools (optional)
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    
    // Actuator (optional)
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## Step 4: Understanding What Each Starter Contains

### spring-boot-starter-web includes:
```
- spring-web (Core Spring Web)
- spring-webmvc (Spring MVC)
- spring-boot-starter-tomcat (Embedded Tomcat)
- spring-boot-starter-json (Jackson for JSON)
- spring-boot-starter (Core Spring Boot)
```

### spring-boot-starter-data-jpa includes:
```
- spring-data-jpa (Spring Data JPA)
- hibernate-core (Hibernate ORM)
- spring-boot-starter-jdbc (JDBC support)
- jakarta.persistence-api (JPA API)
- jakarta.transaction-api (Transaction API)
```

### spring-boot-starter includes (base):
```
- spring-boot (Core)
- spring-boot-autoconfigure (Auto-configuration)
- spring-boot-starter-logging (Logback)
- jakarta.annotation-api (Annotations)
- spring-core, spring-context
```

## Step 5: Dependency Tree Visualization

```
my-spring-boot-app
├── spring-boot-starter-web
│   ├── spring-webmvc
│   ├── spring-web
│   ├── spring-boot-starter-tomcat
│   │   └── tomcat-embed-core
│   └── spring-boot-starter-json
│       └── jackson-databind
├── spring-boot-starter-data-jpa
│   ├── hibernate-core
│   ├── spring-data-jpa
│   └── spring-boot-starter-jdbc
│       └── HikariCP (connection pool)
├── postgresql (JDBC driver)
├── lombok (annotation processor)
└── spring-boot-starter-validation
    └── hibernate-validator
```

## Step 6: Why Manual Setup Matters

### Advantages:
1. **Full Control**: Know exactly what's included
2. **Customization**: Easy to swap implementations
3. **Learning**: Understand Spring Boot internals
4. **Debugging**: Easier to troubleshoot issues
5. **Optimization**: Remove unused dependencies

### What We Avoid:
- ❌ Hidden auto-configuration magic
- ❌ Unnecessary transitive dependencies
- ❌ Version conflicts
- ❌ Bloated JARs

### What We Gain:
- ✅ Explicit dependency declaration
- ✅ Clear understanding of each component
- ✅ Easy to modify or replace
- ✅ Better documentation
- ✅ Reproducible builds

## Step 7: Verifying Dependencies

### View Dependency Tree (Maven)
```bash
mvn dependency:tree
```

### View Dependency Tree (Gradle)
```bash
./gradlew dependencies
```

### Check for Conflicts
```bash
mvn dependency:analyze
```

## Next Steps

1. Create the main application class
2. Configure application properties
3. Create REST controllers
4. Set up database configuration
5. Add service and repository layers

See the implementation files for complete working examples.
