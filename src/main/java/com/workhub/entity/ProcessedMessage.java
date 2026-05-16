package com.workhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Deduplication and reliability ledger for consumed RabbitMQ events.
 * Enforces unique eventId and tracks outcome across retries.
 */
@Entity
@Table(
        name = "processed_messages",
        indexes = {
                @Index(name = "idx_processed_message_event", columnList = "event_id", unique = true),
                @Index(name = "idx_processed_message_status", columnList = "status"),
                @Index(name = "idx_processed_message_tenant", columnList = "tenant_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 100, unique = true)
    private String eventId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProcessedMessageStatus status = ProcessedMessageStatus.PROCESSING;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 1;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @CreationTimestamp
    @Column(name = "first_received_at", nullable = false, updatable = false)
    private OffsetDateTime firstReceivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
