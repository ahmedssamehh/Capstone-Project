package com.workhub.repository;

import com.workhub.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {
    Optional<ProcessedMessage> findByEventId(String eventId);

    Optional<ProcessedMessage> findTopByJobIdOrderByFirstReceivedAtDesc(Long jobId);
}
