package com.workhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Task Entity - Represents a task in the multi-tenant system
 * 
 * Each task belongs to a project and a tenant.
 * Tasks can be assigned to users and have optimistic locking for concurrent updates.
 */
@Entity
@Table(name = "tasks",
    indexes = {
        @Index(name = "idx_task_project", columnList = "project_id"),
        @Index(name = "idx_task_tenant", columnList = "tenant_id"),
        @Index(name = "idx_task_assigned_to", columnList = "assigned_to_id"),
        @Index(name = "idx_task_status", columnList = "status"),
        @Index(name = "idx_task_priority", columnList = "priority")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    /**
     * Primary Key
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /**
     * Many-to-One relationship with Project
     * Every task belongs to exactly one project
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_task_project"))
    private Project project;

    /**
     * Many-to-One relationship with Tenant
     * Denormalized for efficient multi-tenant queries
     * 
     * While tenant can be accessed through project, storing it directly
     * allows for more efficient queries and indexing for tenant isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_task_tenant"))
    private Tenant tenant;

    /**
     * Many-to-One relationship with User (assignee)
     * Task can be assigned to a user
     * 
     * optional = true: Task can be unassigned
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id",
                foreignKey = @ForeignKey(name = "fk_task_assigned_to"))
    private User assignedTo;

    /**
     * Task title
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Task description
     */
    @Column(name = "description", length = 5000)
    private String description;

    /**
     * Task status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    /**
     * Task priority
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    /**
     * Due date
     */
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    /**
     * Estimated hours
     */
    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    /**
     * Actual hours spent
     */
    @Column(name = "actual_hours")
    private Integer actualHours;

    /**
     * VERSION FIELD FOR OPTIMISTIC LOCKING
     * 
     * @Version annotation enables optimistic locking
     * 
     * How it works:
     * 1. When entity is loaded, version is read
     * 2. When entity is updated, version is checked
     * 3. If version changed, OptimisticLockException is thrown
     * 4. Version is automatically incremented on successful update
     * 
     * This prevents lost updates in concurrent scenarios:
     * - User A reads task (version = 1)
     * - User B reads task (version = 1)
     * - User A updates task (version becomes 2)
     * - User B tries to update (version mismatch, exception thrown)
     * 
     * Benefits:
     * - No database locks needed
     * - Better performance than pessimistic locking
     * - Prevents lost updates
     * - Automatic version management
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    /**
     * Completion timestamp
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
     * Task Status Enum
     */
    public enum TaskStatus {
        TODO,
        IN_PROGRESS,
        IN_REVIEW,
        BLOCKED,
        COMPLETED,
        CANCELLED
    }

    /**
     * Task Priority Enum
     */
    public enum TaskPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    /**
     * Convenience method to get tenant ID
     * Critical for multi-tenant queries
     */
    @Transient
    public UUID getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    /**
     * Convenience method to get project ID
     */
    @Transient
    public Long getProjectId() {
        return project != null ? project.getId() : null;
    }

    /**
     * Convenience method to get assignee ID
     */
    @Transient
    public Long getAssignedToId() {
        return assignedTo != null ? assignedTo.getId() : null;
    }

    /**
     * Check if task is completed
     */
    @Transient
    public boolean isCompleted() {
        return status == TaskStatus.COMPLETED;
    }

    /**
     * Check if task is overdue
     */
    @Transient
    public boolean isOverdue() {
        return dueDate != null && 
               LocalDateTime.now().isAfter(dueDate) && 
               !isCompleted();
    }

    /**
     * Mark task as completed
     * Sets completion timestamp and status
     */
    public void markAsCompleted() {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Lifecycle callback - Called before entity is persisted
     * Ensures tenant is set from project if not already set
     */
    @PrePersist
    protected void onCreate() {
        // Ensure tenant is set from project for data consistency
        if (tenant == null && project != null) {
            tenant = project.getTenant();
        }
    }
}
