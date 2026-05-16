package com.workhub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.MDC;

/**
 * RabbitMQ topology for WorkHub.
 *
 * Architecture:
 * - Direct exchange routes messages by exact routing key.
 * - Single durable queue is bound to the exchange.
 * - JSON converter standardizes payload serialization for enterprise integration.
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    @Value("${workhub.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${workhub.rabbitmq.queue}")
    private String queueName;

    @Value("${workhub.rabbitmq.routing-key}")
    private String routingKey;

    @Bean
    public DirectExchange workhubExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue workhubQueue() {
        return new Queue(queueName, true, false, false);
    }

    @Bean
    public Binding workhubBinding(Queue workhubQueue, DirectExchange workhubExchange) {
        return BindingBuilder.bind(workhubQueue).to(workhubExchange).with(routingKey);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);
        template.setExchange(exchangeName);
        template.setRoutingKey(routingKey);
        template.setBeforePublishPostProcessors(message -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null && !correlationId.isBlank()) {
                message.getMessageProperties().setHeader(CorrelationHeader.NAME, correlationId);
            }
            return message;
        });
        return template;
    }

    @Bean
    public org.springframework.boot.ApplicationRunner rabbitStartupVerificationLogger(
            @Value("${spring.rabbitmq.host}") String host,
            @Value("${spring.rabbitmq.port}") int port,
            @Value("${spring.rabbitmq.username}") String username) {
        return args -> log.info(
                "RabbitMQ ready -> host={} port={} user={} exchange={} queue={} routingKey={}",
                host, port, username, exchangeName, queueName, routingKey
        );
    }

    public static final class CorrelationHeader {
        public static final String NAME = "X-Correlation-Id";

        private CorrelationHeader() {
        }
    }
}
