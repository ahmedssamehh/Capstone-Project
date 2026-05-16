package com.workhub.service.impl;

import com.workhub.entity.Job;
import com.workhub.entity.JobStatus;
import com.workhub.messaging.ReportGenerationEvent;
import com.workhub.repository.JobRepository;
import com.workhub.security.CorrelationIdFilter;
import com.workhub.service.MessageIdempotencyService;
import com.workhub.service.ReportGenerationConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer-side processing workflow for report generation events.
 *
 * Retry-safe behavior:
 * - COMPLETED jobs are treated as terminal and ignored.
 * - PROCESSING jobs are ignored to prevent duplicate in-flight work.
 * - PENDING/FAILED jobs move to PROCESSING, then COMPLETED/FAILED.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationConsumerServiceImpl implements ReportGenerationConsumerService {

    private static final int MAX_ERROR_MESSAGE = 2000;

    private final JobRepository jobRepository;
    private final MessageIdempotencyService messageIdempotencyService;

    @Override
    @Transactional(rollbackFor = Exception.class)
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

            try {
                processEvent(event);
                messageIdempotencyService.markCompleted(event.getEventId());
            } catch (Exception ex) {
                messageIdempotencyService.markFailed(event.getEventId(), ex.getMessage());
                throw ex;
            }
        });
    }

    private void processEvent(ReportGenerationEvent event) {
        Job job = jobRepository.findByIdAndTenant_Id(event.getJobId(), event.getTenantId())
                .orElse(null);

        if (job == null) {
            throw new IllegalStateException("Tenant-scoped job not found for event processing");
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            log.info(
                    "Ignoring duplicate event for terminal job eventId={} jobId={} status={}",
                    event.getEventId(), job.getId(), job.getStatus()
            );
            return;
        }

        if (job.getStatus() == JobStatus.PROCESSING) {
            log.info(
                    "Ignoring duplicate event for in-flight job eventId={} jobId={}",
                    event.getEventId(), job.getId()
            );
            return;
        }

        job.setStatus(JobStatus.PROCESSING);
        job.setErrorMessage(null);
        jobRepository.save(job);
        log.info("Job transitioned to PROCESSING jobId={} tenantId={}", job.getId(), job.getTenantId());

        try {
            executeBusinessWorkflow(event);
            job.setStatus(JobStatus.COMPLETED);
            job.setErrorMessage(null);
            jobRepository.save(job);
            log.info("Job transitioned to COMPLETED jobId={} tenantId={}", job.getId(), job.getTenantId());
        } catch (Exception ex) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(truncate(ex.getMessage()));
            jobRepository.save(job);
            log.error(
                    "Job transitioned to FAILED jobId={} tenantId={} reason={}",
                    job.getId(), job.getTenantId(), ex.getMessage()
            );
            throw new RuntimeException("Report workflow execution failed", ex);
        }
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

    private String truncate(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Unknown report processing failure";
        }
        return reason.length() > MAX_ERROR_MESSAGE ? reason.substring(0, MAX_ERROR_MESSAGE) : reason;
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
