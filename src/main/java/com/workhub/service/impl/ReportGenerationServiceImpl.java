package com.workhub.service.impl;

import com.workhub.dto.JobResponse;
import com.workhub.entity.Job;
import com.workhub.entity.JobStatus;
import com.workhub.exception.ResourceNotFoundException;
import com.workhub.messaging.ReportGenerationEvent;
import com.workhub.repository.JobRepository;
import com.workhub.repository.ProjectRepository;
import com.workhub.repository.TenantRepository;
import com.workhub.security.CorrelationIdFilter;
import com.workhub.security.TenantContext;
import com.workhub.service.ReportGenerationProducerService;
import com.workhub.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationServiceImpl implements ReportGenerationService {

    private static final int MAX_ERROR_MESSAGE = 2000;

    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final JobRepository jobRepository;
    private final ReportGenerationProducerService reportGenerationProducerService;
    private final PlatformTransactionManager transactionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobResponse generateReportForProject(Long projectId) {
        Long tenantId = TenantContext.getRequiredTenantId();

        if (!projectRepository.existsByIdAndTenantId(projectId, tenantId)) {
            throw new ResourceNotFoundException("Project not found");
        }

        String correlationId = resolveCorrelationId();
        Job job = Job.builder()
                .tenant(tenantRepository.getReferenceById(tenantId))
                .project(projectRepository.getReferenceById(projectId))
                .status(JobStatus.PENDING)
                .correlationId(correlationId)
                .build();
        Job savedJob = jobRepository.save(job);

        ReportGenerationEvent event = ReportGenerationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .jobId(savedJob.getId())
                .tenantId(tenantId)
                .projectId(projectId)
                .timestamp(OffsetDateTime.now())
                .correlationId(correlationId)
                .build();

        // Publish only after successful DB commit.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    reportGenerationProducerService.publish(event);
                } catch (Exception ex) {
                    log.error(
                            "Post-commit publish failed jobId={} tenantId={} correlationId={} reason={}",
                            savedJob.getId(), tenantId, correlationId, ex.getMessage()
                    );
                    markJobFailedInNewTransaction(savedJob.getId(), tenantId, ex.getMessage());
                }
            }
        });

        log.info(
                "Queued report generation jobId={} tenantId={} projectId={} correlationId={}",
                savedJob.getId(), tenantId, projectId, correlationId
        );
        return JobResponse.fromEntity(savedJob);
    }

    private void markJobFailedInNewTransaction(Long jobId, Long tenantId, String reason) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> {
            jobRepository.findByIdAndTenant_Id(jobId, tenantId).ifPresent(job -> {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage(truncate(reason));
                jobRepository.save(job);
                log.info("Marked job as FAILED after publish failure jobId={} tenantId={}", jobId, tenantId);
            });
        });
    }

    private String resolveCorrelationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletAttributes) {
            Object value = servletAttributes.getRequest().getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
            if (value instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return UUID.randomUUID().toString();
    }

    private String truncate(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Message publication failed";
        }
        return reason.length() > MAX_ERROR_MESSAGE ? reason.substring(0, MAX_ERROR_MESSAGE) : reason;
    }
}
