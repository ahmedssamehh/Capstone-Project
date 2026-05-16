package com.workhub.service.impl;

import com.workhub.messaging.ReportGenerationEvent;
import com.workhub.security.CorrelationIdFilter;
import com.workhub.service.MessageIdempotencyService;
import com.workhub.service.ReportJobStateService;
import com.workhub.service.ReportGenerationConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Consumer-side processing workflow for report generation events.
 *
 * Rollback semantics:
 * - Orchestration is intentionally non-transactional.
 * - Every persistent state transition is executed in REQUIRES_NEW transactions.
 * - Failure evidence (FAILED status + idempotency ledger) cannot be lost by rollback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationConsumerServiceImpl implements ReportGenerationConsumerService {

    private final MessageIdempotencyService messageIdempotencyService;
    private final ReportJobStateService reportJobStateService;

    @Override
    public void consume(ReportGenerationEvent event) {
        withCorrelation(event.getCorrelationId(), () -> {
            MessageIdempotencyService.ProcessingDecision decision = messageIdempotencyService.beginProcessing(event);
            if (decision == MessageIdempotencyService.ProcessingDecision.ALREADY_COMPLETED) {
                log.info("Skipping already-completed event eventId={}", event.getEventId());
                return;
            }
            if (decision == MessageIdempotencyService.ProcessingDecision.ALREADY_PROCESSING) {
                log.info("Skipping duplicate in-flight event eventId={}", event.getEventId());
                return;
            }
            if (decision == MessageIdempotencyService.ProcessingDecision.INVALID_EVENT) {
                log.error("Rejecting invalid event payload for idempotent processing");
                return;
            }

            ReportJobStateService.JobStateTransitionDecision jobDecision =
                    reportJobStateService.markProcessing(event);
            if (jobDecision == ReportJobStateService.JobStateTransitionDecision.ALREADY_COMPLETED
                    || jobDecision == ReportJobStateService.JobStateTransitionDecision.ALREADY_PROCESSING) {
                log.info("Skipping processing due to job state decision={} eventId={}", jobDecision, event.getEventId());
                messageIdempotencyService.markCompleted(event.getEventId());
                return;
            }
            if (jobDecision == ReportJobStateService.JobStateTransitionDecision.NOT_FOUND) {
                String reason = "Tenant-scoped job not found for event processing";
                reportJobStateService.markFailed(event, reason);
                messageIdempotencyService.markFailed(event.getEventId(), reason);
                return;
            }

            try {
                executeBusinessWorkflow(event);
                reportJobStateService.markCompleted(event);
                messageIdempotencyService.markCompleted(event.getEventId());
            } catch (Exception ex) {
                String reason = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                reportJobStateService.markFailed(event, reason);
                messageIdempotencyService.markFailed(event.getEventId(), reason);
                log.error(
                        "Workflow failed with durable failure recording eventId={} jobId={} tenantId={} reason={}",
                        event.getEventId(), event.getJobId(), event.getTenantId(), reason
                );
            }
        });
    }

    /**
     * Core business logic for report processing.
     * Replace this simulation with real report generation pipeline later.
     */
    public void executeBusinessWorkflow(ReportGenerationEvent event) throws InterruptedException {
        log.info(
                "Executing report workflow eventId={} jobId={} projectId={} tenantId={}",
                event.getEventId(), event.getJobId(), event.getProjectId(), event.getTenantId()
        );
        Thread.sleep(300L);
    }

    private void withCorrelation(String correlationId, Runnable action) {
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put(CorrelationIdFilter.ATTRIBUTE_NAME, correlationId);
        }
        try {
            action.run();
        } finally {
            MDC.remove(CorrelationIdFilter.ATTRIBUTE_NAME);
        }
    }
}
