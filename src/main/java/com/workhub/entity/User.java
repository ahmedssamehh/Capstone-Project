package com.workhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User Entity - Represents a user in the multi-tenant system
 * 
 * Each user belongs to exactly one tenant.
 * Users can have different roles within their tenant.
 */
@Entity
@Table(name = "users", 
    indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_tenant", columnList = "tenant_id"),
        @Index(name = "idx_user_role", columnList = "role")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_email", columnNames = "email")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Primary Key - Using Long for simplicity and performance
     * IDENTITY strategy works well with most databases
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /**
     * Many-to-One relationship with Tenant
     * Every user belongs to exactly one tenant
     * 
     * @ManyToOne: Many users can belong to one tenant
     * @JoinColumn: Specifies the foreign key column
     * fetch = LAZY: Don't load tenant unless explicitly accessed
     * optional = false: User must have a tenant
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_user_tenant"))
    private Tenant tenant;

    /**
     * User email - Must be unique across all tenants
     * Used for authentication
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Password hash - Never store plain text passwords
     * Should be hashed using BCrypt or similar
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * User role within the tenant
     * Determines permissions and access levels
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    @Builder.Default
    private UserRole role = UserRole.TENANT_USER;

    /**
     * User status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * First name
     */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /**
     * Last name
     */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /**
     * One-to-Many relationship with Projects (as creator)
     * A user can create multiple projects
     */
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Project> createdProjects = new HashSet<>();

    /**
     * One-to-Many relationship with Tasks (as assignee)
     * A user can be assigned to multiple tasks
     */
    @OneToMany(mappedBy = "assignedTo", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Task> assignedTasks = new HashSet<>();

    /**
     * Last login timestamp
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * VERSION FIELD FOR OPTIMISTIC LOCKING
     * 
     * Prevents concurrent modification issues when multiple sessions
     * update the same user simultaneously (e.g., profile updates, role changes)
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    /**
     * Creation timestamp
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Update timestamp
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User Role Enum
     * Defines the role hierarchy within a tenant
     */
    public enum UserRole {
        /**
         * Tenant administrator - Full access to tenant resources
         * Can manage users, projects, and settings
         */
        TENANT_ADMIN,
        
        /**
         * Regular tenant user - Limited access
         * Can view and work on assigned projects/tasks
         */
        TENANT_USER
    }

    /**
     * User Status Enum
     */
    public enum UserStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }

    /**
     * Convenience method to get tenant ID
     * Useful for queries and validation
     */
    @Transient
    public Long getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    /**
     * Convenience method to get full name
     */
    @Transient
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return email;
    }

    /**
     * Check if user is admin
     */
    @Transient
    public boolean isAdmin() {
        return role == UserRole.TENANT_ADMIN;
    }
}
