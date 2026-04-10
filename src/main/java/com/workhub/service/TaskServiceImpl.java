package com.workhub.service;

import com.workhub.dto.CreateTaskRequest;
import com.workhub.dto.TaskDto;
import com.workhub.dto.UpdateTaskRequest;
import com.workhub.entity.Project;
import com.workhub.entity.Task;
import com.workhub.entity.User;
import com.workhub.repository.ProjectRepository;
import com.workhub.repository.TaskRepository;
import com.workhub.repository.UserRepository;
import com.workhub.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    public TaskDto createTask(CreateTaskRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        log.info("Creating task for project: {} in tenant: {}", request.getProjectId(), tenantId);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));
        
        validateTenantAccess(project.getTenant().getId());

        Task.TaskBuilder taskBuilder = Task.builder()
                .project(project)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Task.TaskPriority.MEDIUM)
                .status(Task.TaskStatus.TODO)
                .dueDate(request.getDueDate())
                .estimatedHours(request.getEstimatedHours());

        if (request.getAssignedToId() != null) {
            User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getAssignedToId()));
            taskBuilder.assignedTo(assignee);
        }

        Task savedTask = taskRepository.save(taskBuilder.build());
        return mapToDto(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        
        validateTenantAccess(task.getProject().getTenant().getId());
        return mapToDto(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasksByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        
        validateTenantAccess(project.getTenant().getId());
        
        return taskRepository.findByProjectId(projectId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasksByTenant(UUID tenantId) {
        validateTenantAccess(tenantId);
        return taskRepository.findByTenantId(tenantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByAssignee(Long userId) {
        return taskRepository.findByAssignedToId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByProjectAndStatus(Long projectId, Task.TaskStatus status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        
        validateTenantAccess(project.getTenant().getId());
        
        return taskRepository.findByProjectIdAndStatus(projectId, status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByAssigneeAndStatus(Long userId, Task.TaskStatus status) {
        return taskRepository.findByAssignedToIdAndStatus(userId, status).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getOverdueTasks() {
        List<Task.TaskStatus> excludedStatuses = Arrays.asList(Task.TaskStatus.COMPLETED);
        return taskRepository.findOverdueTasks(LocalDateTime.now(), excludedStatuses).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public TaskDto updateTask(Long id, UpdateTaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        
        validateTenantAccess(task.getProject().getTenant().getId());

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
            if (request.getStatus() == Task.TaskStatus.COMPLETED && task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getAssignedToId() != null) {
            User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getAssignedToId()));
            task.setAssignedTo(assignee);
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getEstimatedHours() != null) {
            task.setEstimatedHours(request.getEstimatedHours());
        }
        if (request.getActualHours() != null) {
            task.setActualHours(request.getActualHours());
        }

        Task updatedTask = taskRepository.save(task);
        return mapToDto(updatedTask);
    }

    @Override
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        
        validateTenantAccess(task.getProject().getTenant().getId());
        taskRepository.delete(task);
        log.info("Deleted task: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTasksByProject(Long projectId) {
        return taskRepository.countByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTasksByProjectAndStatus(Long projectId, Task.TaskStatus status) {
        return taskRepository.countByProjectIdAndStatus(projectId, status);
    }

    private void validateTenantAccess(UUID tenantId) {
        UUID currentTenantId = TenantContext.getCurrentTenantId();
        if (!tenantId.equals(currentTenantId)) {
            throw new SecurityException("Access denied to tenant: " + tenantId);
        }
    }

    private TaskDto mapToDto(Task task) {
        return TaskDto.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToName(task.getAssignedTo() != null ? 
                        task.getAssignedTo().getFirstName() + " " + task.getAssignedTo().getLastName() : null)
                .createdById(task.getCreatedBy() != null ? task.getCreatedBy().getId() : null)
                .createdByName(task.getCreatedBy() != null ? 
                        task.getCreatedBy().getFirstName() + " " + task.getCreatedBy().getLastName() : null)
                .dueDate(task.getDueDate())
                .estimatedHours(task.getEstimatedHours())
                .actualHours(task.getActualHours())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
