package com.workhub.service;

import com.workhub.dto.CreateJobRequest;
import com.workhub.dto.JobResponse;
import com.workhub.entity.JobStatus;

import java.util.List;

public interface JobService {

    JobResponse enqueueJob(CreateJobRequest request);

    JobResponse getJobById(Long jobId);

    List<JobResponse> listJobs(JobStatus status);
}
