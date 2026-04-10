package com.workhub.controller;

import com.workhub.dto.CreateProjectRequest;
import com.workhub.dto.CreateTaskRequest;
import com.workhub.dto.ProjectResponse;
import com.workhub.dto.TaskResponse;
import com.workhub.entity.Project;
import com.workhub.entity.Task;
import com.workhub.security.CustomUserDetails;
import com.workhub.service.ProjectService;
import com.workhub.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Project operations
 * 
 * All operations are automatically tenant-scoped via TenantContext
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    /**
     * Create a new project
     * 
     * POST /api/projects
     * 
     * @param request Project creation request (validated)
     * @param currentUser Authenticated user
     * @return Created project
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        
        log.info("Creating project: {} by user: {}", request.getName(), currentUser.getEmail());

        // Convert DTO to entity
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectKey(request.getProjectKey())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        // Create project (tenant assigned automatically)
        Project createdProject = projectService.createProject(project, currentUser.getId());

        // Convert entity to DTO
        ProjectResponse response = ProjectResponse.fromEntity(createdProject);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all projects for current tenant
     * 
     * GET /api/projects
     * 
     * @return List of projects
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        log.debug("Fetching all projects");

        List<Project> projects = projectService.getAllProjects();

        // Convert entities to DTOs
        List<ProjectResponse> response = projects.stream()
                .map(ProjectResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get project by ID
     * 
     * GET /api/projects/{id}
     * 
     * @param id Project ID
     * @return Project details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        log.debug("Fetching project: {}", id);

        return projectService.getProjectById(id)
                .map(ProjectResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a task for a project
     * 
     * POST /api/projects/{id}/tasks
     * 
     * @param id Project ID
     * @param request Task creation request (validated)
     * @return Created task
     */
    @PostMapping("/{id}/tasks")
    public ResponseEntity<TaskResponse> createTaskForProject(
            @PathVariable Long id,
            @Valid @RequestBody CreateTaskRequest request) {
        
        log.info("Creating task for project: {}", id);

        // Verify project exists and belongs to tenant
        if (!projectService.projectExists(id)) {
            return ResponseEntity.notFound().build();
        }

        // Convert DTO to entity
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .dueDate(request.getDueDate())
                .estimatedHours(request.getEstimatedHours())
                .build();

        // Set assignee if provided
        if (request.getAssignedToId() != null) {
            // Note: TaskService will validate assignee belongs to tenant
            Task tempTask = new Task();
            tempTask.setId(request.getAssignedToId());
            task.setAssignedTo(com.workhub.entity.User.builder()
                    .id(request.getAssignedToId())
                    .build());
        }

        // Create task (tenant assigned automatically)
        Task createdTask = taskService.createTask(task, id);

        // Convert entity to DTO
        TaskResponse response = TaskResponse.fromEntity(createdTask);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
