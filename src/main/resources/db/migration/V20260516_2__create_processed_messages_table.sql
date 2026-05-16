-- Idempotency ledger for RabbitMQ consumer reliability.
-- Guarantees unique eventId processing with retry-safe state tracking.

CREATE TABLE IF NOT EXISTS processed_messages (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    correlation_id VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 1,
    last_error VARCHAR(2000),
    first_received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_processed_message_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_processed_message_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT fk_processed_message_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_processed_message_event
    ON processed_messages(event_id);
CREATE INDEX IF NOT EXISTS idx_processed_message_status
    ON processed_messages(status);
CREATE INDEX IF NOT EXISTS idx_processed_message_tenant
    ON processed_messages(tenant_id);
