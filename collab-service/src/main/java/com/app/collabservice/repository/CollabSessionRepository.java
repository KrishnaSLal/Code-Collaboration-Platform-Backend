package com.app.collabservice.repository;

import com.app.collabservice.entity.CollabSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollabSessionRepository extends JpaRepository<CollabSession, String> {

    Optional<CollabSession> findBySessionId(String sessionId);

    List<CollabSession> findByProjectId(Long projectId);

    List<CollabSession> findByFileId(Long fileId);

    List<CollabSession> findByProjectIdAndStatus(Long projectId, String status);

    List<CollabSession> findByOwnerId(Long ownerId);
}