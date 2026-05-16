package com.workhub.service;

import com.workhub.messaging.ReportGenerationEvent;

public interface ReportGenerationConsumerService {
    void consume(ReportGenerationEvent event);
}
