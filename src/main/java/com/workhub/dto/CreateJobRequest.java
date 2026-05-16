package com.workhub.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to enqueue an asynchronous report processing job.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobRequest {

    @NotNull(message = "Project ID is required")
    @Positive(message = "Project ID must be positive")
    private Long projectId;
}
