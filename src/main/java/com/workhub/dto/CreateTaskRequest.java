package com.workhub.dto;

import com.workhub.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {

    @NotNull(message = "Project ID is required")
    private Long projectId;

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    private Task.TaskPriority priority;

    private Long assignedToId;

    private LocalDateTime dueDate;

    private Integer estimatedHours;
}
