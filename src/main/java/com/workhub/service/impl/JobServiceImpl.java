package com.workhub.service.impl;

import com.workhub.dto.CreateJobRequest;
import com.workhub.dto.JobResponse;
import com.workhub.entity.Job;
import com.workhub.entity.JobStatus;
import com.workhub.exception.ResourceNotFoundException;
import com.workhub.messaging.ReportGenerationEvent;
import com.workhub.repository.JobRepository;
import com.workhub.repository.ProjectRepository;
import com.workhub.repository.TenantRepository;
import com.workhub.security.TenantContext;
import com.workhub.service.JobService;
import com.workhub.service.ReportGenerationProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class JobServiceImpl implements JobService {

    private static final int MAX_ERROR_MESSAGE = 2000;

    private final JobRepository jobRepository;
    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final ReportGenerationProducerService reportGenerationProducerService;

    @Override
    public JobResponse enqueueJob(CreateJobRequest request) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Long projectId = request.getProjectId();

        if (!projectRepository.existsByIdAndTenantId(projectId, tenantId)) {
            throw new ResourceNotFoundException("Project not found");
        }

        Job job = Job.builder()
                .tenant(tenantRepository.getReferenceById(tenantId))
                .project(projectRepository.getReferenceById(projectId))
                .status(JobStatus.PENDING)
                .correlationId(UUID.randomUUID().toString())
                .build();

        Job saved = jobRepository.save(job);

        ReportGenerationEvent event = ReportGenerationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .jobId(saved.getId())
                .tenantId(tenantId)
                .projectId(projectId)
                .timestamp(OffsetDateTime.now())
                .correlationId(saved.getCorrelationId())
                .build();

        reportGenerationProducerService.publish(event);
        log.info(
                "Enqueued async job id={} tenantId={} projectId={} correlationId={}",
                saved.getId(), tenantId, projectId, saved.getCorrelationId()
        );

        return JobResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long jobId) {
        Long tenantId = TenantContext.getRequiredTenantId();
        Job job = jobRepository.findByIdAndTenant_Id(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        return JobResponse.fromEntity(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(JobStatus status) {
        Long tenantId = TenantContext.getRequiredTenantId();
        List<Job> jobs = (status == null)
                ? jobRepository.findAllByTenant_IdOrderByCreatedAtDesc(tenantId)
                : jobRepository.findAllByTenant_IdAndStatusOrderByCreatedAtDesc(tenantId, status);

        return jobs.stream().map(JobResponse::fromEntity).toList();
    }

    /**
     * Consumer-side processing pipeline.
     *
     * Trust boundary:
     * - This method never reads tenantId from HTTP context.
     * - It uses tenantId from the message and enforces tenant-safe repository lookup.
     */
    @RabbitListener(queues = "${workhub.rabbitmq.queue}")
    public void processJobMessage(ReportGenerationEvent message) {
        log.info(
                "Received report generation event eventId={} jobId={} tenantId={} projectId={} correlationId={}",
                message.getEventId(), message.getJobId(), message.getTenantId(), message.getProjectId(), message.getCorrelationId()
        );

        try {
            markJobProcessing(message.getJobId(), message.getTenantId());
            executeReportProcessing(message);
            markJobCompleted(message.getJobId(), message.getTenantId());
        } catch (Exception ex) {
            log.error(
                    "Job processing failed jobId={} tenantId={} correlationId={} reason={}",
                    message.getJobId(), message.getTenantId(), message.getCorrelationId(), ex.getMessage()
            );
            markJobFailed(message.getJobId(), message.getTenantId(), ex.getMessage());
        }
    }

    @Transactional
    protected void markJobProcessing(Long jobId, Long tenantId) {
        Job job = loadTenantScopedJob(jobId, tenantId);
        if (job.getStatus() != JobStatus.PENDING) {
            log.warn("Ignoring non-pending job id={} tenantId={} status={}", jobId, tenantId, job.getStatus());
            return;
        }
        job.setStatus(JobStatus.PROCESSING);
        job.setErrorMessage(null);
        jobRepository.save(job);
        log.info("Job moved to PROCESSING id={} tenantId={}", jobId, tenantId);
    }

    @Transactional
    protected void markJobCompleted(Long jobId, Long tenantId) {
        Job job = loadTenantScopedJob(jobId, tenantId);
        job.setStatus(JobStatus.COMPLETED);
        job.setErrorMessage(null);
        jobRepository.save(job);
        log.info("Job moved to COMPLETED id={} tenantId={}", jobId, tenantId);
    }

    @Transactional
    protected void markJobFailed(Long jobId, Long tenantId, String reason) {
        Job job = loadTenantScopedJob(jobId, tenantId);
        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(truncate(reason));
        jobRepository.save(job);
        log.info("Job moved to FAILED id={} tenantId={}", jobId, tenantId);
    }

    @Transactional(readOnly = true)
    protected Job loadTenantScopedJob(Long jobId, Long tenantId) {
        return jobRepository.findByIdAndTenant_Id(jobId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    /**
     * Simulates asynchronous report generation workload.
     */
    protected void executeReportProcessing(ReportGenerationEvent message) throws InterruptedException {
        log.info(
                "Processing report asynchronously for job={} project={} tenant={} correlationId={}",
                message.getJobId(), message.getProjectId(), message.getTenantId(), message.getCorrelationId()
        );
        Thread.sleep(300L);
    }

    private String truncate(String reason) {
        if (reason == null) {
            return "Unknown processing error";
        }
        return reason.length() > MAX_ERROR_MESSAGE ? reason.substring(0, MAX_ERROR_MESSAGE) : reason;
    }
}
