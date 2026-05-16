package com.workhub.dto;

import com.workhub.entity.Job;
import com.workhub.entity.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API response for async job state.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobResponse {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private JobStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String correlationId;
    private String errorMessage;

    public static JobResponse fromEntity(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .projectId(job.getProjectId())
                .status(job.getStatus())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .correlationId(job.getCorrelationId())
                .errorMessage(job.getErrorMessage())
                .build();
    }
}
