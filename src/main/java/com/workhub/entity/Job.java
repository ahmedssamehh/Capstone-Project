package com.workhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Job entity for asynchronous report processing.
 *
 * Tenant and project references are stored as mandatory relations to enforce
 * foreign key integrity and tenant-safe lookup patterns.
 */
@Entity
@Table(
        name = "jobs",
        indexes = {
                @Index(name = "idx_job_tenant", columnList = "tenant_id"),
                @Index(name = "idx_job_project", columnList = "project_id"),
                @Index(name = "idx_job_status", columnList = "status"),
                @Index(name = "idx_job_correlation_id", columnList = "correlation_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_job_tenant"))
    private Tenant tenant;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_job_project"))
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Transient
    public Long getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    @Transient
    public Long getProjectId() {
        return project != null ? project.getId() : null;
    }
}
