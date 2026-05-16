package com.workhub.repository;

import com.workhub.entity.Job;
import com.workhub.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Job repository with mandatory tenant-safe query methods.
 */
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findByIdAndTenant_Id(Long id, Long tenantId);

    List<Job> findAllByTenant_IdOrderByCreatedAtDesc(Long tenantId);

    List<Job> findAllByTenant_IdAndStatusOrderByCreatedAtDesc(Long tenantId, JobStatus status);

    boolean existsByIdAndTenant_Id(Long id, Long tenantId);
}
