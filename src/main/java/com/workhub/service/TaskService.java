package com.workhub.service;

import com.workhub.entity.Project;
import com.workhub.entity.Task;
import com.workhub.entity.Tenant;
import com.workhub.entity.User;
import com.workhub.repository.ProjectRepository;
import com.workhub.repository.TaskRepository;
import com.workhub.repository.TenantRepository;
import com.workhub.repository.UserRepository;
import com.workhub.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Task Service
 * 
 * Business logic layer for Task operations.
 * 
 * TENANT ISOLATION GUARANTEE:
 * - All operations automatically use tenantId from TenantContext
 * - No cross-tenant access is possible
 * - All repository calls explicitly filter by tenantId
 * - Validation ensures entities belong to current tenant
 * - Tasks have denormalized tenant_id for efficient querying
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    // ========================================
    // READ OPERATIONS
    // ========================================

    /**
     * Get all tasks for current tenant
     * 
     * @return List of tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        Long tenantId = getTenantId();
        log.debug("Fetching all tasks for tenant: {}", tenantId);
        return taskRepository.findByTenantId(tenantId);
    }

    /**
     * Get task by ID for current tenant
     * 
     * @param taskId Task ID
     * @return Task if found and belongs to current tenant
     */
    @Transactional(readOnly = true)
    public Optional<Task> getTaskById(Long taskId) {
        Long tenantId = getTenantId();
        log.debug("Fetching task {} for tenant: {}", taskId, tenantId);
        return taskRepository.findByIdAndTenantId(taskId, tenantId);
    }

    /**
     * Get task by ID or throw exception
     * 
     * @param taskId Task ID
     * @return Task
     * @throws IllegalArgumentException if task not found or doesn't belong to tenant
     */
    @Transactional(readOnly = true)
    public Task getTaskByIdOrThrow(Long taskId) {
        return getTaskById(taskId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Task not found or access denied: " + taskId));
    }

    /**
     * Get tasks by project
     * 
     * @param projectId Project ID
     * @return List of tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByProject(Long projectId) {
        Long tenantId = getTenantId();
        validateProjectBelongsToTenant(projectId, tenantId);
        log.debug("Fetching tasks for project {} and tenant: {}", projectId, tenantId);
        return taskRepository.findByProjectIdAndTenantId(projectId, tenantId);
    }

    /**
     * Get tasks by project and status
     * 
     * @param projectId Project ID
     * @param status Task status
     * @return List of tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByProjectAndStatus(Long projectId, Task.TaskStatus status) {
        Long tenantId = getTenantId();
        validateProjectBelongsToTenant(projectId, tenantId);
        return taskRepository.findByProjectIdAndTenantIdAndStatus(projectId, tenantId, status);
    }

    /**
     * Get tasks by status
     * 
     * @param status Task status
     * @return List of tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByStatus(Task.TaskStatus status) {
        Long tenantId = getTenantId();
        log.debug("Fetching tasks with status {} for tenant: {}", status, tenantId);
        return taskRepository.findByTenantIdAndStatus(tenantId, status);
    }

    /**
     * Get TODO tasks
     * 
     * @return List of TODO tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getTodoTasks() {
        Long tenantId = getTenantId();
        return taskRepository.findTodoTasksByTenantId(tenantId);
    }

    /**
     * Get in-progress tasks
     * 
     * @return List of in-progress tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getInProgressTasks() {
        Long tenantId = getTenantId();
        return taskRepository.findInProgressTasksByTenantId(tenantId);
    }

    /**
     * Get completed tasks
     * 
     * @return List of completed tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getCompletedTasks() {
        Long tenantId = getTenantId();
        return taskRepository.findCompletedTasksByTenantId(tenantId);
    }

    /**
     * Get tasks by priority
     * 
     * @param priority Task priority
     * @return List of tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByPriority(Task.TaskPriority priority) {
        Long tenantId = getTenantId();
        return taskRepository.findByTenantIdAndPriority(tenantId, priority);
    }

    /**
     * Get urgent tasks
     * 
     * @return List of urgent tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getUrgentTasks() {
        Long tenantId = getTenantId();
        return taskRepository.findUrgentTasksByTenantId(tenantId);
    }

    /**
     * Get tasks assigned to user
     * 
     * @param userId User ID
     * @return List of tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByAssignee(Long userId) {
        Long tenantId = getTenantId();
        validateUserBelongsToTenant(userId, tenantId);
        return taskRepository.findByAssignedToIdAndTenantId(userId, tenantId);
    }

    /**
     * Get tasks assigned to user with status
     * 
     * @param userId User ID
     * @param status Task status
     * @return List of tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksByAssigneeAndStatus(Long userId, Task.TaskStatus status) {
        Long tenantId = getTenantId();
        validateUserBelongsToTenant(userId, tenantId);
        return taskRepository.findByAssignedToIdAndTenantIdAndStatus(userId, tenantId, status);
    }

    /**
     * Get unassigned tasks
     * 
     * @return List of unassigned tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getUnassignedTasks() {
        Long tenantId = getTenantId();
        return taskRepository.findUnassignedTasksByTenantId(tenantId);
    }

    /**
     * Get overdue tasks
     * 
     * @return List of overdue tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getOverdueTasks() {
        Long tenantId = getTenantId();
        return taskRepository.findOverdueTasksByTenantId(tenantId, LocalDateTime.now());
    }

    /**
     * Get tasks due before date
     * 
     * @param date Date threshold
     * @return List of tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksDueBefore(LocalDateTime date) {
        Long tenantId = getTenantId();
        return taskRepository.findTasksDueBefore(tenantId, date);
    }

    /**
     * Search tasks by title
     * 
     * @param title Title search string
     * @return List of matching tasks
     */
    @Transactional(readOnly = true)
    public List<Task> searchTasksByTitle(String title) {
        Long tenantId = getTenantId();
        log.debug("Searching tasks by title '{}' for tenant: {}", title, tenantId);
        return taskRepository.searchByTitleInTenant(tenantId, title);
    }

    // ========================================
    // CREATE OPERATIONS
    // ========================================

    /**
     * Create new task for current tenant
     * 
     * @param task Task to create
     * @param projectId Project ID
     * @return Created task
     */
    public Task createTask(Task task, Long projectId) {
        Long tenantId = getTenantId();
        log.info("Creating task '{}' for project {} and tenant: {}", 
                task.getTitle(), projectId, tenantId);

        // Validate tenant isolation
        validateTenantIsolation(task, tenantId);
        
        // Validate project belongs to tenant
        validateProjectBelongsToTenant(projectId, tenantId);

        // Validate assignee if provided
        if (task.getAssignedTo() != null) {
            validateUserBelongsToTenant(task.getAssignedTo().getId(), tenantId);
        }

        // Set tenant and project (with validation)
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        
        task.setTenant(tenant);
        task.setProject(project);
        
        // Set default status if not provided
        if (task.getStatus() == null) {
            task.setStatus(Task.TaskStatus.TODO);
        }
        
        // Set default priority if not provided
        if (task.getPriority() == null) {
            task.setPriority(Task.TaskPriority.MEDIUM);
        }

        Task savedTask = taskRepository.save(task);
        log.info("Created task {} for tenant: {}", savedTask.getId(), tenantId);
        
        return savedTask;
    }

    // ========================================
    // UPDATE OPERATIONS
    // ========================================

    /**
     * Update task for current tenant
     * 
     * @param taskId Task ID
     * @param updatedTask Updated task data
     * @return Updated task
     */
    public Task updateTask(Long taskId, Task updatedTask) {
        Long tenantId = getTenantId();
        log.info("Updating task {} for tenant: {}", taskId, tenantId);

        // Fetch existing task (ensures tenant ownership)
        Task existingTask = getTaskByIdOrThrow(taskId);

        // Validate tenant isolation
        validateTenantIsolation(updatedTask, tenantId);

        // Validate project if changed
        if (updatedTask.getProject() != null && 
            !updatedTask.getProject().getId().equals(existingTask.getProject().getId())) {
            validateProjectBelongsToTenant(updatedTask.getProject().getId(), tenantId);
        }

        // Validate assignee if changed
        if (updatedTask.getAssignedTo() != null) {
            validateUserBelongsToTenant(updatedTask.getAssignedTo().getId(), tenantId);
        }

        // Update fields
        existingTask.setTitle(updatedTask.getTitle());
        existingTask.setDescription(updatedTask.getDescription());
        existingTask.setStatus(updatedTask.getStatus());
        existingTask.setPriority(updatedTask.getPriority());
        existingTask.setDueDate(updatedTask.getDueDate());
        existingTask.setEstimatedHours(updatedTask.getEstimatedHours());
        existingTask.setActualHours(updatedTask.getActualHours());
        
        if (updatedTask.getAssignedTo() != null) {
            existingTask.setAssignedTo(updatedTask.getAssignedTo());
        }

        // Set completed timestamp if status changed to COMPLETED
        if (updatedTask.getStatus() == Task.TaskStatus.COMPLETED && 
            existingTask.getCompletedAt() == null) {
            existingTask.setCompletedAt(LocalDateTime.now());
        }

        Task savedTask = taskRepository.save(existingTask);
        log.info("Updated task {} for tenant: {}", taskId, tenantId);
        
        return savedTask;
    }

    /**
     * Update task status
     * 
     * @param taskId Task ID
     * @param status New status
     * @return Updated task
     */
    public Task updateTaskStatus(Long taskId, Task.TaskStatus status) {
        Long tenantId = getTenantId();
        log.info("Updating task {} status to {} for tenant: {}", taskId, status, tenantId);

        Task task = getTaskByIdOrThrow(taskId);
        task.setStatus(status);

        // Set completed timestamp if status is COMPLETED
        if (status == Task.TaskStatus.COMPLETED && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        }

        return taskRepository.save(task);
    }

    /**
     * Assign task to user
     * 
     * @param taskId Task ID
     * @param userId User ID
     * @return Updated task
     */
    public Task assignTask(Long taskId, Long userId) {
        Long tenantId = getTenantId();
        log.info("Assigning task {} to user {} for tenant: {}", taskId, userId, tenantId);

        Task task = getTaskByIdOrThrow(taskId);
        validateUserBelongsToTenant(userId, tenantId);

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        task.setAssignedTo(user);

        return taskRepository.save(task);
    }

    /**
     * Unassign task
     * 
     * @param taskId Task ID
     * @return Updated task
     */
    public Task unassignTask(Long taskId) {
        Long tenantId = getTenantId();
        log.info("Unassigning task {} for tenant: {}", taskId, tenantId);

        Task task = getTaskByIdOrThrow(taskId);
        task.setAssignedTo(null);

        return taskRepository.save(task);
    }

    /**
     * Update task priority
     * 
     * @param taskId Task ID
     * @param priority New priority
     * @return Updated task
     */
    public Task updateTaskPriority(Long taskId, Task.TaskPriority priority) {
        Long tenantId = getTenantId();
        log.info("Updating task {} priority to {} for tenant: {}", taskId, priority, tenantId);

        Task task = getTaskByIdOrThrow(taskId);
        task.setPriority(priority);

        return taskRepository.save(task);
    }

    /**
     * Move task to different project
     * 
     * @param taskId Task ID
     * @param newProjectId New project ID
     * @return Updated task
     */
    public Task moveTaskToProject(Long taskId, Long newProjectId) {
        Long tenantId = getTenantId();
        log.info("Moving task {} to project {} for tenant: {}", taskId, newProjectId, tenantId);

        Task task = getTaskByIdOrThrow(taskId);
        validateProjectBelongsToTenant(newProjectId, tenantId);

        Project newProject = projectRepository.findByIdAndTenantId(newProjectId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + newProjectId));
        task.setProject(newProject);

        return taskRepository.save(task);
    }

    // ========================================
    // DELETE OPERATIONS
    // ========================================

    /**
     * Delete task for current tenant
     * 
     * @param taskId Task ID
     * @return true if deleted, false if not found
     */
    public boolean deleteTask(Long taskId) {
        Long tenantId = getTenantId();
        log.info("Deleting task {} for tenant: {}", taskId, tenantId);

        if (taskRepository.existsByIdAndTenantId(taskId, tenantId)) {
            taskRepository.deleteByIdAndTenantId(taskId, tenantId);
            log.info("Deleted task {} for tenant: {}", taskId, tenantId);
            return true;
        }
        
        log.warn("Task {} not found for tenant: {}", taskId, tenantId);
        return false;
    }

    // ========================================
    // STATISTICS & COUNTS
    // ========================================

    /**
     * Count tasks for current tenant
     * 
     * @return Number of tasks
     */
    @Transactional(readOnly = true)
    public long countTasks() {
        Long tenantId = getTenantId();
        return taskRepository.countByTenantId(tenantId);
    }

    /**
     * Count tasks by project
     * 
     * @param projectId Project ID
     * @return Number of tasks
     */
    @Transactional(readOnly = true)
    public long countTasksByProject(Long projectId) {
        Long tenantId = getTenantId();
        validateProjectBelongsToTenant(projectId, tenantId);
        return taskRepository.countByProjectIdAndTenantId(projectId, tenantId);
    }

    /**
     * Count tasks by status
     * 
     * @param status Task status
     * @return Number of tasks
     */
    @Transactional(readOnly = true)
    public long countTasksByStatus(Task.TaskStatus status) {
        Long tenantId = getTenantId();
        return taskRepository.countByTenantIdAndStatus(tenantId, status);
    }

    /**
     * Count tasks assigned to user
     * 
     * @param userId User ID
     * @return Number of tasks
     */
    @Transactional(readOnly = true)
    public long countTasksByAssignee(Long userId) {
        Long tenantId = getTenantId();
        validateUserBelongsToTenant(userId, tenantId);
        return taskRepository.countByAssignedToIdAndTenantId(userId, tenantId);
    }

    /**
     * Count overdue tasks
     * 
     * @return Number of overdue tasks
     */
    @Transactional(readOnly = true)
    public long countOverdueTasks() {
        Long tenantId = getTenantId();
        return taskRepository.countOverdueTasksByTenantId(tenantId, LocalDateTime.now());
    }

    /**
     * Get tasks over budget (actual hours > estimated hours)
     * 
     * @return List of tasks
     */
    @Transactional(readOnly = true)
    public List<Task> getTasksOverBudget() {
        Long tenantId = getTenantId();
        return taskRepository.findTasksOverBudget(tenantId);
    }

    // ========================================
    // VALIDATION METHODS
    // ========================================

    /**
     * Check if task exists for current tenant
     * 
     * @param taskId Task ID
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean taskExists(Long taskId) {
        Long tenantId = getTenantId();
        return taskRepository.existsByIdAndTenantId(taskId, tenantId);
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    /**
     * Get current tenant ID from context
     * 
     * @return Tenant ID
     * @throws IllegalStateException if tenant context not set
     */
    private Long getTenantId() {
        return TenantContext.getRequiredTenantId();
    }

    /**
     * Validate that task belongs to current tenant
     * Prevents cross-tenant data manipulation
     * 
     * @param task Task to validate
     * @param tenantId Expected tenant ID
     * @throws IllegalArgumentException if tenant mismatch
     */
    private void validateTenantIsolation(Task task, Long tenantId) {
        if (task.getTenantId() != null && !task.getTenantId().equals(tenantId)) {
            log.error("Tenant isolation violation: task tenant {} != context tenant {}", 
                     task.getTenantId(), tenantId);
            throw new IllegalArgumentException(
                "Task tenant ID does not match current tenant context");
        }
    }

    /**
     * Validate that project belongs to current tenant
     * 
     * @param projectId Project ID
     * @param tenantId Tenant ID
     * @throws IllegalArgumentException if project doesn't belong to tenant
     */
    private void validateProjectBelongsToTenant(Long projectId, Long tenantId) {
        if (!projectRepository.existsByIdAndTenantId(projectId, tenantId)) {
            log.error("Project {} does not belong to tenant {}", projectId, tenantId);
            throw new IllegalArgumentException(
                "Project does not belong to current tenant");
        }
    }

    /**
     * Validate that user belongs to current tenant
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @throws IllegalArgumentException if user doesn't belong to tenant
     */
    private void validateUserBelongsToTenant(Long userId, Long tenantId) {
        if (!userRepository.findByIdAndTenantId(userId, tenantId).isPresent()) {
            log.error("User {} does not belong to tenant {}", userId, tenantId);
            throw new IllegalArgumentException(
                "User does not belong to current tenant");
        }
    }
}
