package com.workhub.dto;

import com.workhub.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for task response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private Long projectId;
    private String projectName;
    private Long tenantId;
    private Long assignedToId;
    private String assignedToName;
    private LocalDateTime dueDate;
    private Integer estimatedHours;
    private Integer actualHours;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert Task entity to TaskResponse DTO
     */
    public static TaskResponse fromEntity(Task task) {
        TaskResponseBuilder builder = TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .tenantId(task.getTenantId())
                .dueDate(task.getDueDate())
                .estimatedHours(task.getEstimatedHours())
                .actualHours(task.getActualHours())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt());

        // Add assignee info if present
        if (task.getAssignedTo() != null) {
            builder.assignedToId(task.getAssignedTo().getId())
                   .assignedToName(task.getAssignedTo().getFirstName() + " " + task.getAssignedTo().getLastName());
        }

        return builder.build();
    }
}
