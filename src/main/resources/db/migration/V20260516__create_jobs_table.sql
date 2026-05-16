-- Schema update for asynchronous job/report processing.
-- If Flyway is enabled later, this migration can be applied as-is.

CREATE TABLE IF NOT EXISTS jobs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id VARCHAR(100) NOT NULL,
    error_message VARCHAR(2000),
    CONSTRAINT fk_job_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_job_project FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE INDEX IF NOT EXISTS idx_job_tenant ON jobs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_job_project ON jobs(project_id);
CREATE INDEX IF NOT EXISTS idx_job_status ON jobs(status);
CREATE INDEX IF NOT EXISTS idx_job_correlation_id ON jobs(correlation_id);
