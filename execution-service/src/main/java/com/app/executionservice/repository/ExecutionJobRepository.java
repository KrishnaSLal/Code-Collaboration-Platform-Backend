package com.app.executionservice.repository;

import com.app.executionservice.entity.ExecutionJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExecutionJobRepository extends JpaRepository<ExecutionJob, String> {

    Optional<ExecutionJob> findByJobId(String jobId);

    List<ExecutionJob> findByUserId(Long userId);

    List<ExecutionJob> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);

    List<ExecutionJob> findByStatus(String status);

    List<ExecutionJob> findByLanguage(String language);

    List<ExecutionJob> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByUserId(Long userId);

    long countByStatus(String status);

    long countByUserIdAndStatus(Long userId, String status);
}
