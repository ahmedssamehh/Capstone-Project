package com.workhub.service;

import com.workhub.messaging.ReportGenerationEvent;

public interface ReportJobStateService {

    JobStateTransitionDecision markProcessing(ReportGenerationEvent event);

    void markCompleted(ReportGenerationEvent event);

    void markFailed(ReportGenerationEvent event, String reason);

    enum JobStateTransitionDecision {
        PROCESSING_STARTED,
        ALREADY_COMPLETED,
        ALREADY_PROCESSING,
        NOT_FOUND
    }
}
