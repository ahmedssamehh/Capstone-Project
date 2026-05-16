package com.workhub.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Domain event published when a report generation job is requested.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportGenerationEvent {
    private String eventId;
    private Long jobId;
    private Long tenantId;
    private Long projectId;
    private OffsetDateTime timestamp;
    private String correlationId;
}
