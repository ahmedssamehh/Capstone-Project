package com.workhub.messaging;

import com.workhub.service.ReportGenerationConsumerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Thin RabbitMQ listener that delegates all business behavior to the consumer service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationEventListener {

    private final ReportGenerationConsumerService reportGenerationConsumerService;

    @RabbitListener(queues = "${workhub.rabbitmq.queue}")
    public void onReportGenerationEvent(ReportGenerationEvent event) {
        log.info(
                "Received ReportGenerationEvent eventId={} jobId={} tenantId={} projectId={} correlationId={}",
                event.getEventId(), event.getJobId(), event.getTenantId(), event.getProjectId(), event.getCorrelationId()
        );
        reportGenerationConsumerService.consume(event);
    }
}
