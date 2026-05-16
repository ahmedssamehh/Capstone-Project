package com.workhub.service.impl;

import com.workhub.exception.MessagePublishException;
import com.workhub.messaging.ReportGenerationEvent;
import com.workhub.service.ReportGenerationProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationProducerServiceImpl implements ReportGenerationProducerService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${workhub.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${workhub.rabbitmq.routing-key}")
    private String routingKey;

    @Override
    public void publish(ReportGenerationEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            log.info(
                    "Published ReportGenerationEvent eventId={} jobId={} tenantId={} projectId={} correlationId={}",
                    event.getEventId(), event.getJobId(), event.getTenantId(), event.getProjectId(), event.getCorrelationId()
            );
        } catch (AmqpException ex) {
            log.error(
                    "Failed to publish ReportGenerationEvent eventId={} jobId={} tenantId={} reason={}",
                    event.getEventId(), event.getJobId(), event.getTenantId(), ex.getMessage()
            );
            throw new MessagePublishException("Failed to publish report generation event");
        }
    }
}
