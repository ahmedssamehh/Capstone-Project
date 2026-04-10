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
 * Project Entity - Represents a project in the multi-tenant system
 * 
 * Each project belongs to exactly one tenant and is created by a user.
 * Projects contain multiple tasks.
 */
@Entity
@Table(name = "projects",
    indexes = {
        @Index(name = "idx_project_tenant", columnList = "tenant_id"),
        @Index(name = "idx_project_created_by", columnList = "created_by_id"),
        @Index(name = "idx_project_status", columnList = "status"),
        @Index(name = "idx_project_name", columnList = "name")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    /**
     * Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /**
     * Many-to-One relationship with Tenant
     * Every project belongs to exactly one tenant
     * 
     * This is crucial for multi-tenant data isolation
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_project_tenant"))
    private Tenant tenant;

    /**
     * Many-to-One relationship with User (creator)
     * Tracks who created the project
     * 
     * fetch = LAZY: Don't load user unless explicitly accessed
     * optional = false: Project must have a creator
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_project_created_by"))
    private User createdBy;

    /**
     * Project name
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Project description
     */
    @Column(name = "description", length = 2000)
    private String description;

    /**
     * Project key - Short identifier (e.g., "PROJ", "WEB")
     * Useful for task references like "PROJ-123"
     */
    @Column(name = "project_key", length = 10)
    private String projectKey;

    /**
     * Project status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.ACTIVE;

    /**
     * One-to-Many relationship with Tasks
     * A project can have multiple tasks
     * 
     * cascade = ALL: Operations on project cascade to tasks
     * orphanRemoval = true: Delete tasks when removed from project
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();

    /**
     * Project start date
     */
    @Column(name = "start_date")
    private LocalDateTime startDate;

    /**
     * Project end date
     */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /**
     * VERSION FIELD FOR OPTIMISTIC LOCKING
     * 
     * Prevents concurrent modification issues when multiple users
     * update the same project simultaneously (e.g., status changes, name updates)
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
     * Project Status Enum
     */
    public enum ProjectStatus {
        ACTIVE,
        ON_HOLD,
        COMPLETED,
        ARCHIVED,
        CANCELLED
    }

    /**
     * Convenience method to get tenant ID
     * Essential for multi-tenant queries and validation
     */
    @Transient
    public Long getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    /**
     * Convenience method to get creator ID
     */
    @Transient
    public Long getCreatedById() {
        return createdBy != null ? createdBy.getId() : null;
    }

    /**
     * Helper method to add task to project
     * Maintains bidirectional relationship
     */
    public void addTask(Task task) {
        tasks.add(task);
        task.setProject(this);
    }

    /**
     * Helper method to remove task from project
     */
    public void removeTask(Task task) {
        tasks.remove(task);
        task.setProject(null);
    }

    /**
     * Get task count
     */
    @Transient
    public int getTaskCount() {
        return tasks != null ? tasks.size() : 0;
    }

    /**
     * Check if project is active
     */
    @Transient
    public boolean isActive() {
        return status == ProjectStatus.ACTIVE;
    }
}
