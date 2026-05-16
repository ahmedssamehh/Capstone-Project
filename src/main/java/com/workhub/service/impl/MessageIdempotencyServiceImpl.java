package com.workhub.service.impl;

import com.workhub.entity.ProcessedMessage;
import com.workhub.entity.ProcessedMessageStatus;
import com.workhub.messaging.ReportGenerationEvent;
import com.workhub.repository.ProcessedMessageRepository;
import com.workhub.service.MessageIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageIdempotencyServiceImpl implements MessageIdempotencyService {

    private static final int MAX_ERROR_MESSAGE = 2000;

    private final ProcessedMessageRepository processedMessageRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ProcessingDecision beginProcessing(ReportGenerationEvent event) {
        if (!isValidEvent(event)) {
            return ProcessingDecision.INVALID_EVENT;
        }

        ProcessedMessage existing = processedMessageRepository.findByEventId(event.getEventId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == ProcessedMessageStatus.COMPLETED) {
                return ProcessingDecision.ALREADY_COMPLETED;
            }
            if (existing.getStatus() == ProcessedMessageStatus.PROCESSING) {
                return ProcessingDecision.ALREADY_PROCESSING;
            }
            existing.setStatus(ProcessedMessageStatus.PROCESSING);
            existing.setLastError(null);
            existing.setAttemptCount(existing.getAttemptCount() + 1);
            processedMessageRepository.save(existing);
            return ProcessingDecision.PROCEED;
        }

        ProcessedMessage created = ProcessedMessage.builder()
                .eventId(event.getEventId())
                .tenantId(event.getTenantId())
                .jobId(event.getJobId())
                .projectId(event.getProjectId())
                .correlationId(event.getCorrelationId())
                .status(ProcessedMessageStatus.PROCESSING)
                .attemptCount(1)
                .build();
        try {
            processedMessageRepository.save(created);
            return ProcessingDecision.PROCEED;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent listener/thread reserved this event first.
            log.info("Duplicate event reservation detected eventId={}", event.getEventId());
            return ProcessingDecision.ALREADY_PROCESSING;
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markCompleted(String eventId) {
        processedMessageRepository.findByEventId(eventId).ifPresent(pm -> {
            pm.setStatus(ProcessedMessageStatus.COMPLETED);
            pm.setLastError(null);
            pm.setProcessedAt(OffsetDateTime.now());
            processedMessageRepository.save(pm);
        });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(String eventId, String reason) {
        processedMessageRepository.findByEventId(eventId).ifPresent(pm -> {
            pm.setStatus(ProcessedMessageStatus.FAILED);
            pm.setLastError(truncate(reason));
            pm.setProcessedAt(OffsetDateTime.now());
            processedMessageRepository.save(pm);
        });
    }

    private boolean isValidEvent(ReportGenerationEvent event) {
        return event != null
                && event.getEventId() != null && !event.getEventId().isBlank()
                && event.getTenantId() != null
                && event.getJobId() != null
                && event.getProjectId() != null;
    }

    private String truncate(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Unknown processing failure";
        }
        return reason.length() > MAX_ERROR_MESSAGE ? reason.substring(0, MAX_ERROR_MESSAGE) : reason;
    }
}
