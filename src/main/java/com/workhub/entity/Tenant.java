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
import java.util.UUID;

/**
 * Tenant Entity - Represents a tenant organization in the multi-tenant system
 * 
 * Each tenant is a separate organization with its own users, projects, and tasks.
 * This is the root entity for tenant isolation.
 */
@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenant_name", columnList = "name"),
    @Index(name = "idx_tenant_plan", columnList = "plan")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    /**
     * Primary Key - Using UUID for better security and distribution
     * UUID prevents enumeration attacks and works well in distributed systems
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Tenant name - Organization or company name
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Subscription plan - Determines features and limits
     * Using ENUM for type safety
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 50)
    @Builder.Default
    private TenantPlan plan = TenantPlan.FREE;

    /**
     * Tenant status - Active, suspended, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private TenantStatus status = TenantStatus.ACTIVE;

    /**
     * One-to-Many relationship with Users
     * mappedBy: Refers to the 'tenant' field in User entity
     * cascade: Operations on Tenant cascade to Users
     * orphanRemoval: Delete users when removed from tenant
     */
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<User> users = new HashSet<>();

    /**
     * One-to-Many relationship with Projects
     */
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Project> projects = new HashSet<>();

    /**
     * VERSION FIELD FOR OPTIMISTIC LOCKING
     * 
     * Prevents concurrent modification issues when multiple users
     * update the same tenant simultaneously
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    /**
     * Automatic timestamp for creation
     * @CreationTimestamp: Hibernate automatically sets this on insert
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatic timestamp for updates
     * @UpdateTimestamp: Hibernate automatically updates this on update
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Tenant Plan Enum
     */
    public enum TenantPlan {
        FREE,
        STARTER,
        PROFESSIONAL,
        ENTERPRISE
    }

    /**
     * Tenant Status Enum
     */
    public enum TenantStatus {
        ACTIVE,
        SUSPENDED,
        TRIAL,
        EXPIRED
    }

    /**
     * Helper method to add user to tenant
     * Maintains bidirectional relationship
     */
    public void addUser(User user) {
        users.add(user);
        user.setTenant(this);
    }

    /**
     * Helper method to remove user from tenant
     */
    public void removeUser(User user) {
        users.remove(user);
        user.setTenant(null);
    }

    /**
     * Helper method to add project to tenant
     */
    public void addProject(Project project) {
        projects.add(project);
        project.setTenant(this);
    }

    /**
     * Helper method to remove project from tenant
     */
    public void removeProject(Project project) {
        projects.remove(project);
        project.setTenant(null);
    }
}
