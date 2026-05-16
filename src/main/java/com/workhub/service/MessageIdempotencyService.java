package com.workhub.service;

import com.workhub.messaging.ReportGenerationEvent;

public interface MessageIdempotencyService {

    ProcessingDecision beginProcessing(ReportGenerationEvent event);

    void markCompleted(String eventId);

    void markFailed(String eventId, String reason);

    enum ProcessingDecision {
        PROCEED,
        ALREADY_COMPLETED,
        ALREADY_PROCESSING,
        INVALID_EVENT
    }
}
