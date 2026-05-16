package com.workhub.service;

import com.workhub.dto.JobResponse;

public interface ReportGenerationService {
    JobResponse generateReportForProject(Long projectId);
}
