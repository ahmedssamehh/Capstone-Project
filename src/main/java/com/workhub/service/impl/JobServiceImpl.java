package com.workhub.service.impl;

import com.workhub.dto.CreateJobRequest;
import com.workhub.dto.JobResponse;
import com.workhub.entity.Job;
import com.workhub.entity.JobStatus;
import com.workhub.exception.ResourceNotFoundException;
import com.workhub.repository.JobRepository;
import com.workhub.security.TenantContext;
import com.workhub.service.JobService;
import com.workhub.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final ReportGenerationService reportGenerationService;

    @Override
    public JobResponse enqueueJob(CreateJobRequest request) {
        return reportGenerationService.generateReportForProject(request.getProjectId());
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

}
