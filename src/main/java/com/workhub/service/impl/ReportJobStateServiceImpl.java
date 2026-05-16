package com.workhub.service.impl;

import com.workhub.entity.Job;
import com.workhub.entity.JobStatus;
import com.workhub.messaging.ReportGenerationEvent;
import com.workhub.repository.JobRepository;
import com.workhub.service.ReportJobStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportJobStateServiceImpl implements ReportJobStateService {

    private static final int MAX_ERROR_MESSAGE = 2000;

    private final JobRepository jobRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public JobStateTransitionDecision markProcessing(ReportGenerationEvent event) {
        Job job = jobRepository.findByIdAndTenant_Id(event.getJobId(), event.getTenantId()).orElse(null);
        if (job == null) {
            return JobStateTransitionDecision.NOT_FOUND;
        }
        if (job.getStatus() == JobStatus.COMPLETED) {
            return JobStateTransitionDecision.ALREADY_COMPLETED;
        }
        if (job.getStatus() == JobStatus.PROCESSING) {
            return JobStateTransitionDecision.ALREADY_PROCESSING;
        }
        job.setStatus(JobStatus.PROCESSING);
        job.setErrorMessage(null);
        jobRepository.save(job);
        log.info("Job transitioned to PROCESSING jobId={} tenantId={}", job.getId(), job.getTenantId());
        return JobStateTransitionDecision.PROCESSING_STARTED;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markCompleted(ReportGenerationEvent event) {
        jobRepository.findByIdAndTenant_Id(event.getJobId(), event.getTenantId()).ifPresent(job -> {
            job.setStatus(JobStatus.COMPLETED);
            job.setErrorMessage(null);
            jobRepository.save(job);
            log.info("Job transitioned to COMPLETED jobId={} tenantId={}", job.getId(), job.getTenantId());
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(ReportGenerationEvent event, String reason) {
        jobRepository.findByIdAndTenant_Id(event.getJobId(), event.getTenantId()).ifPresent(job -> {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(truncate(reason));
            jobRepository.save(job);
            log.error(
                    "Job transitioned to FAILED jobId={} tenantId={} reason={}",
                    job.getId(), job.getTenantId(), reason
            );
        });
    }

    private String truncate(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Unknown report processing failure";
        }
        return reason.length() > MAX_ERROR_MESSAGE ? reason.substring(0, MAX_ERROR_MESSAGE) : reason;
    }
}
