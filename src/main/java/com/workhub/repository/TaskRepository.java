package com.workhub.repository;

import com.workhub.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Task Repository - Data access layer for Task entity
 * 
 * ALL QUERIES FILTER BY TENANT ID for multi-tenant isolation.
 * This ensures tasks are never leaked across tenants.
 * 
 * Tasks have denormalized tenant_id for efficient querying without joins.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // ========================================
    // TENANT-FILTERED QUERIES (REQUIRED)
    // ========================================

    /**
     * Find all tasks by tenant ID
     * 
     * ALWAYS use this instead of findAll() to ensure tenant isolation
     * Uses denormalized tenant_id for fast querying
     * 
     * @param tenantId Tenant ID
     * @return List of tasks in the tenant
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId")
    List<Task> findAllByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Find task by ID and tenant ID
     * 
     * ALWAYS use this instead of findById() to ensure tenant isolation
     * 
     * @param id Task ID
     * @param tenantId Tenant ID
     * @return Optional containing task if found in tenant
     */
    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.tenant.id = :tenantId")
    Optional<Task> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    /**
     * Check if task exists by ID and tenant ID
     * 
     * @param id Task ID
     * @param tenantId Tenant ID
     * @return true if task exists in tenant
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
           "FROM Task t WHERE t.id = :id AND t.tenant.id = :tenantId")
    boolean existsByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    /**
     * Delete task by ID and tenant ID
     * 
     * ALWAYS use this instead of deleteById() to ensure tenant isolation
     * 
     * @param id Task ID
     * @param tenantId Tenant ID
     */
    @Query("DELETE FROM Task t WHERE t.id = :id AND t.tenant.id = :tenantId")
    void deleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    // ========================================
    // PROJECT-BASED QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Find tasks by project ID and tenant ID
     * 
     * @param projectId Project ID
     * @param tenantId Tenant ID
     * @return List of tasks in the project
     */
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.tenant.id = :tenantId")
    List<Task> findByProjectIdAndTenantId(
        @Param("projectId") Long projectId, 
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find tasks by project ID, tenant ID, and status
     * 
     * @param projectId Project ID
     * @param tenantId Tenant ID
     * @param status Task status
     * @return List of matching tasks
     */
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId " +
           "AND t.tenant.id = :tenantId AND t.status = :status")
    List<Task> findByProjectIdAndTenantIdAndStatus(
        @Param("projectId") Long projectId,
        @Param("tenantId") UUID tenantId,
        @Param("status") Task.TaskStatus status
    );

    /**
     * Find tasks by project ID, tenant ID, and priority
     * 
     * @param projectId Project ID
     * @param tenantId Tenant ID
     * @param priority Task priority
     * @return List of matching tasks
     */
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId " +
           "AND t.tenant.id = :tenantId AND t.priority = :priority")
    List<Task> findByProjectIdAndTenantIdAndPriority(
        @Param("projectId") Long projectId,
        @Param("tenantId") UUID tenantId,
        @Param("priority") Task.TaskPriority priority
    );

    // ========================================
    // STATUS-BASED QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Find tasks by tenant ID and status
     * 
     * @param tenantId Tenant ID
     * @param status Task status
     * @return List of tasks with the specified status
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId AND t.status = :status")
    List<Task> findByTenantIdAndStatus(
        @Param("tenantId") UUID tenantId, 
        @Param("status") Task.TaskStatus status
    );

    /**
     * Find TODO tasks in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of TODO tasks
     */
    default List<Task> findTodoTasksByTenantId(UUID tenantId) {
        return findByTenantIdAndStatus(tenantId, Task.TaskStatus.TODO);
    }

    /**
     * Find in-progress tasks in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of in-progress tasks
     */
    default List<Task> findInProgressTasksByTenantId(UUID tenantId) {
        return findByTenantIdAndStatus(tenantId, Task.TaskStatus.IN_PROGRESS);
    }

    /**
     * Find completed tasks in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of completed tasks
     */
    default List<Task> findCompletedTasksByTenantId(UUID tenantId) {
        return findByTenantIdAndStatus(tenantId, Task.TaskStatus.COMPLETED);
    }

    // ========================================
    // PRIORITY-BASED QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Find tasks by tenant ID and priority
     * 
     * @param tenantId Tenant ID
     * @param priority Task priority
     * @return List of tasks with the specified priority
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId AND t.priority = :priority")
    List<Task> findByTenantIdAndPriority(
        @Param("tenantId") UUID tenantId, 
        @Param("priority") Task.TaskPriority priority
    );

    /**
     * Find urgent tasks in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of urgent tasks
     */
    default List<Task> findUrgentTasksByTenantId(UUID tenantId) {
        return findByTenantIdAndPriority(tenantId, Task.TaskPriority.URGENT);
    }

    /**
     * Find high priority tasks in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of high priority tasks
     */
    default List<Task> findHighPriorityTasksByTenantId(UUID tenantId) {
        return findByTenantIdAndPriority(tenantId, Task.TaskPriority.HIGH);
    }

    // ========================================
    // ASSIGNEE-BASED QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Find tasks by assignee and tenant ID
     * 
     * @param assignedToId Assignee user ID
     * @param tenantId Tenant ID
     * @return List of tasks assigned to the user
     */
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :assignedToId AND t.tenant.id = :tenantId")
    List<Task> findByAssignedToIdAndTenantId(
        @Param("assignedToId") Long assignedToId, 
        @Param("tenantId") UUID tenantId
    );

    /**
     * Find tasks by assignee, tenant ID, and status
     * 
     * @param assignedToId Assignee user ID
     * @param tenantId Tenant ID
     * @param status Task status
     * @return List of matching tasks
     */
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :assignedToId " +
           "AND t.tenant.id = :tenantId AND t.status = :status")
    List<Task> findByAssignedToIdAndTenantIdAndStatus(
        @Param("assignedToId") Long assignedToId,
        @Param("tenantId") UUID tenantId,
        @Param("status") Task.TaskStatus status
    );

    /**
     * Find unassigned tasks in tenant
     * 
     * @param tenantId Tenant ID
     * @return List of unassigned tasks
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId AND t.assignedTo IS NULL")
    List<Task> findUnassignedTasksByTenantId(@Param("tenantId") UUID tenantId);

    // ========================================
    // DATE-BASED QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Find overdue tasks in tenant
     * 
     * @param tenantId Tenant ID
     * @param now Current date/time
     * @return List of overdue tasks
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId " +
           "AND t.dueDate < :now AND t.status != 'COMPLETED'")
    List<Task> findOverdueTasksByTenantId(
        @Param("tenantId") UUID tenantId, 
        @Param("now") LocalDateTime now
    );

    /**
     * Find tasks due before specified date
     * 
     * @param tenantId Tenant ID
     * @param date Date threshold
     * @return List of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId " +
           "AND t.dueDate IS NOT NULL AND t.dueDate < :date")
    List<Task> findTasksDueBefore(
        @Param("tenantId") UUID tenantId, 
        @Param("date") LocalDateTime date
    );

    /**
     * Find tasks due between dates
     * 
     * @param tenantId Tenant ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId " +
           "AND t.dueDate BETWEEN :startDate AND :endDate")
    List<Task> findTasksDueBetween(
        @Param("tenantId") UUID tenantId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find tasks completed after specified date
     * 
     * @param tenantId Tenant ID
     * @param date Date threshold
     * @return List of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId " +
           "AND t.completedAt IS NOT NULL AND t.completedAt > :date")
    List<Task> findTasksCompletedAfter(
        @Param("tenantId") UUID tenantId, 
        @Param("date") LocalDateTime date
    );

    // ========================================
    // SEARCH QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Search tasks by title in tenant
     * 
     * @param tenantId Tenant ID
     * @param title Title search string
     * @return List of matching tasks
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId " +
           "AND LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Task> searchByTitleInTenant(
        @Param("tenantId") UUID tenantId, 
        @Param("title") String title
    );

    /**
     * Search tasks by description in tenant
     * 
     * @param tenantId Tenant ID
     * @param description Description search string
     * @return List of matching tasks
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId " +
           "AND LOWER(t.description) LIKE LOWER(CONCAT('%', :description, '%'))")
    List<Task> searchByDescriptionInTenant(
        @Param("tenantId") UUID tenantId, 
        @Param("description") String description
    );

    // ========================================
    // COUNT QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Count tasks by tenant ID
     * 
     * @param tenantId Tenant ID
     * @return Number of tasks in tenant
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * Count tasks by project ID and tenant ID
     * 
     * @param projectId Project ID
     * @param tenantId Tenant ID
     * @return Number of tasks in the project
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.project.id = :projectId AND t.tenant.id = :tenantId")
    long countByProjectIdAndTenantId(
        @Param("projectId") Long projectId, 
        @Param("tenantId") UUID tenantId
    );

    /**
     * Count tasks by tenant ID and status
     * 
     * @param tenantId Tenant ID
     * @param status Task status
     * @return Number of tasks with the specified status
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.tenant.id = :tenantId AND t.status = :status")
    long countByTenantIdAndStatus(
        @Param("tenantId") UUID tenantId, 
        @Param("status") Task.TaskStatus status
    );

    /**
     * Count tasks by assignee and tenant ID
     * 
     * @param assignedToId Assignee user ID
     * @param tenantId Tenant ID
     * @return Number of tasks assigned to the user
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.assignedTo.id = :assignedToId AND t.tenant.id = :tenantId")
    long countByAssignedToIdAndTenantId(
        @Param("assignedToId") Long assignedToId, 
        @Param("tenantId") UUID tenantId
    );

    /**
     * Count overdue tasks in tenant
     * 
     * @param tenantId Tenant ID
     * @param now Current date/time
     * @return Number of overdue tasks
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.tenant.id = :tenantId " +
           "AND t.dueDate < :now AND t.status != 'COMPLETED'")
    long countOverdueTasksByTenantId(
        @Param("tenantId") UUID tenantId, 
        @Param("now") LocalDateTime now
    );

    // ========================================
    // COMPLEX QUERIES (TENANT-FILTERED)
    // ========================================

    /**
     * Find tasks by multiple statuses in tenant
     * 
     * @param tenantId Tenant ID
     * @param statuses List of task statuses
     * @return List of matching tasks
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId AND t.status IN :statuses")
    List<Task> findByTenantIdAndStatusIn(
        @Param("tenantId") UUID tenantId, 
        @Param("statuses") List<Task.TaskStatus> statuses
    );

    /**
     * Find tasks by project, status, and priority
     * 
     * @param projectId Project ID
     * @param tenantId Tenant ID
     * @param status Task status
     * @param priority Task priority
     * @return List of matching tasks
     */
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId " +
           "AND t.tenant.id = :tenantId AND t.status = :status AND t.priority = :priority")
    List<Task> findByProjectIdAndTenantIdAndStatusAndPriority(
        @Param("projectId") Long projectId,
        @Param("tenantId") UUID tenantId,
        @Param("status") Task.TaskStatus status,
        @Param("priority") Task.TaskPriority priority
    );

    /**
     * Find tasks with estimated hours greater than specified value
     * 
     * @param tenantId Tenant ID
     * @param hours Minimum estimated hours
     * @return List of tasks
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId " +
           "AND t.estimatedHours IS NOT NULL AND t.estimatedHours > :hours")
    List<Task> findTasksWithEstimatedHoursGreaterThan(
        @Param("tenantId") UUID tenantId, 
        @Param("hours") int hours
    );

    /**
     * Find tasks where actual hours exceed estimated hours
     * 
     * @param tenantId Tenant ID
     * @return List of tasks over budget
     */
    @Query("SELECT t FROM Task t WHERE t.tenant.id = :tenantId " +
           "AND t.estimatedHours IS NOT NULL AND t.actualHours IS NOT NULL " +
           "AND t.actualHours > t.estimatedHours")
    List<Task> findTasksOverBudget(@Param("tenantId") UUID tenantId);
}
