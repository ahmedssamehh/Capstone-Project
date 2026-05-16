package com.workhub.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Queue payload for asynchronous job execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobProcessingMessage {
    private Long jobId;
    private Long tenantId;
    private Long projectId;
    private String correlationId;
}
