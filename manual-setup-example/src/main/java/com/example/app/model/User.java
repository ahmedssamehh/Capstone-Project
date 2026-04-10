package com.example.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Entity - JPA Entity with Lombok annotations
 * 
 * This demonstrates:
 * 1. JPA annotations for database mapping
 * 2. Lombok annotations for reducing boilerplate
 * 3. Bean Validation annotations for data validation
 */

/**
 * @Entity - Marks this class as a JPA entity
 * - Will be mapped to a database table
 * - Requires @Id field
 * - Managed by EntityManager
 */
@Entity
/**
 * @Table - Specifies table details
 * - name: table name in database (optional, defaults to class name)
 * - indexes: database indexes for performance
 */
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email")
})
/**
 * Lombok @Data - Generates at compile time:
 * - @Getter for all fields
 * - @Setter for all non-final fields
 * - @ToString
 * - @EqualsAndHashCode
 * - @RequiredArgsConstructor
 * 
 * This replaces ~50 lines of boilerplate code
 */
@Data
/**
 * Lombok @NoArgsConstructor
 * - Generates no-argument constructor
 * - Required by JPA
 */
@NoArgsConstructor
/**
 * Lombok @AllArgsConstructor
 * - Generates constructor with all fields
 * - Useful for testing
 */
@AllArgsConstructor
/**
 * Lombok @Builder
 * - Generates builder pattern
 * - Allows: User.builder().name("John").email("john@example.com").build()
 * - Cleaner than constructors with many parameters
 */
@Builder
public class User {

    /**
     * Primary Key
     * 
     * @Id - Marks this field as primary key
     * @GeneratedValue - Auto-generate value
     * - strategy = IDENTITY: Use database auto-increment
     * - Alternative: AUTO, SEQUENCE, TABLE, UUID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Name field with validation
     * 
     * @Column - Specifies column details
     * - nullable = false: NOT NULL constraint
     * - length: VARCHAR length
     * 
     * @NotBlank - Bean Validation
     * - Must not be null
     * - Must not be empty
     * - Must contain at least one non-whitespace character
     */
    @Column(nullable = false, length = 100)
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    /**
     * Email field with validation and unique constraint
     * 
     * @Column unique = true: UNIQUE constraint in database
     * @Email: Validates email format
     */
    @Column(nullable = false, unique = true, length = 150)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /**
     * Age field with validation
     * 
     * No @Column means default settings:
     * - Column name = field name
     * - Nullable = true
     * - Type inferred from Java type
     */
    private Integer age;

    /**
     * Active status
     * 
     * Boolean fields are typically mapped to:
     * - PostgreSQL: BOOLEAN
     * - MySQL: TINYINT(1)
     * - Oracle: NUMBER(1)
     */
    @Column(nullable = false)
    @Builder.Default  // Lombok: use this value in builder
    private Boolean active = true;

    /**
     * Timestamps
     * 
     * @Column updatable = false: Never update this column
     * This is important for createdAt - should never change
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA Lifecycle Callbacks
     * 
     * @PrePersist - Called before entity is persisted (INSERT)
     * @PreUpdate - Called before entity is updated (UPDATE)
     * 
     * These methods automatically set timestamps
     * No need to manually set them in service layer
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

/**
 * What Lombok generates for this class:
 * 
 * 1. Getters for all fields:
 *    public Long getId() { return id; }
 *    public String getName() { return name; }
 *    ... etc
 * 
 * 2. Setters for all non-final fields:
 *    public void setId(Long id) { this.id = id; }
 *    public void setName(String name) { this.name = name; }
 *    ... etc
 * 
 * 3. toString() method:
 *    public String toString() {
 *        return "User(id=" + id + ", name=" + name + ", email=" + email + ")";
 *    }
 * 
 * 4. equals() and hashCode() methods
 * 
 * 5. No-arg constructor:
 *    public User() {}
 * 
 * 6. All-args constructor:
 *    public User(Long id, String name, String email, ...) { ... }
 * 
 * 7. Builder class:
 *    public static class UserBuilder { ... }
 *    public static UserBuilder builder() { return new UserBuilder(); }
 * 
 * Total: ~150 lines of code generated from ~10 lines of annotations
 */
