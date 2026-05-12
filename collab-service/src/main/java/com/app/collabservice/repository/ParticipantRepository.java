package com.app.collabservice.repository;

import com.app.collabservice.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findBySessionId(String sessionId);

    Optional<Participant> findBySessionIdAndUserId(String sessionId, Long userId);

    long countBySessionId(String sessionId);

    long countBySessionIdAndLeftAtIsNull(String sessionId);
}
