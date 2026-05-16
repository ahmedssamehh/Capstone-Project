package com.workhub.controller;

import com.workhub.dto.TaskResponse;
import com.workhub.dto.UpdateTaskRequest;
import com.workhub.entity.Task;
import com.workhub.entity.User;
import com.workhub.exception.ResourceNotFoundException;
import com.workhub.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Task operations
 * 
 * All operations are automatically tenant-scoped via TenantContext
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('TENANT_ADMIN','TENANT_USER')")
public class TaskController {

    private final TaskService taskService;

    /**
     * Update a task (partial update)
     * 
     * PATCH /api/tasks/{id}
     * 
     * @param id Task ID
     * @param request Task update request (validated, all fields optional)
     * @return Updated task
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        
        log.info("Updating task: {}", id);

        // Get existing task (validates tenant ownership)
        Task existingTask = taskService.getTaskByIdOrThrow(id);

        // Apply updates (only non-null fields)
        if (request.getTitle() != null) {
            existingTask.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            existingTask.setDescription(request.getDescription());
        }

        if (request.getStatus() != null) {
            existingTask.setStatus(request.getStatus());
        }

        if (request.getPriority() != null) {
            existingTask.setPriority(request.getPriority());
        }

        if (request.getDueDate() != null) {
            existingTask.setDueDate(request.getDueDate());
        }

        if (request.getEstimatedHours() != null) {
            existingTask.setEstimatedHours(request.getEstimatedHours());
        }

        if (request.getActualHours() != null) {
            existingTask.setActualHours(request.getActualHours());
        }

        if (request.getAssignedToId() != null) {
            // TaskService will validate assignee belongs to tenant
            User assignee = User.builder()
                    .id(request.getAssignedToId())
                    .build();
            existingTask.setAssignedTo(assignee);
        }

        // Update task (tenant validated automatically)
        Task updatedTask = taskService.updateTask(id, existingTask);

        // Convert entity to DTO
        TaskResponse response = TaskResponse.fromEntity(updatedTask);

        return ResponseEntity.ok(response);
    }

    /**
     * Get task by ID
     * 
     * GET /api/tasks/{id}
     * 
     * @param id Task ID
     * @return Task details
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        log.debug("Fetching task: {}", id);

        return taskService.getTaskById(id)
                .map(TaskResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    /**
     * Delete task
     * 
     * DELETE /api/tasks/{id}
     * 
     * @param id Task ID
     * @return No content if deleted, not found if doesn't exist
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        log.info("Deleting task: {}", id);

        boolean deleted = taskService.deleteTask(id);

        if (!deleted) {
            throw new ResourceNotFoundException("Task not found");
        }
        return ResponseEntity.noContent().build();
    }
}
