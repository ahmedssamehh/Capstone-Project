package com.workhub.dto;

import com.workhub.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for creating a new task
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(min = 3, max = 500, message = "Task title must be between 3 and 500 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private Task.TaskPriority priority;

    private LocalDateTime dueDate;

    @Positive(message = "Estimated hours must be positive")
    private Integer estimatedHours;

    private Long assignedToId;
}
