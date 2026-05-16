package com.workhub.service;

import com.workhub.messaging.ReportGenerationEvent;

public interface ReportGenerationProducerService {
    void publish(ReportGenerationEvent event);
}
