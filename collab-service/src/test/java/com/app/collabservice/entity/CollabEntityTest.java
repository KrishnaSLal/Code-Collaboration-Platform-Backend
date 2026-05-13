package com.app.collabservice.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CollabEntityTest {

    @Test
    void collabSessionOnCreateInitializesMissingDefaults() {
        CollabSession session = CollabSession.builder()
                .projectId(1L)
                .fileId(2L)
                .ownerId(3L)
                .build();

        session.onCreate();

        assertThat(session.getSessionId()).isNotBlank();
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getStatus()).isEqualTo("ACTIVE");
        assertThat(session.getPasswordProtected()).isFalse();
    }

    @Test
    void collabSessionOnCreatePreservesExistingValues() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        CollabSession session = CollabSession.builder()
                .sessionId("session-1")
                .projectId(1L)
                .fileId(2L)
                .ownerId(3L)
                .createdAt(createdAt)
                .status("ENDED")
                .passwordProtected(true)
                .build();

        session.onCreate();

        assertThat(session.getSessionId()).isEqualTo("session-1");
        assertThat(session.getCreatedAt()).isEqualTo(createdAt);
        assertThat(session.getStatus()).isEqualTo("ENDED");
        assertThat(session.getPasswordProtected()).isTrue();
    }

    @Test
    void participantOnCreateInitializesMissingDefaults() {
        Participant participant = Participant.builder()
                .sessionId("session-1")
                .userId(8L)
                .role("EDITOR")
                .build();

        participant.onCreate();

        assertThat(participant.getJoinedAt()).isNotNull();
        assertThat(participant.getCursorLine()).isEqualTo(1);
        assertThat(participant.getCursorCol()).isEqualTo(1);
    }

    @Test
    void participantOnCreatePreservesExistingValues() {
        LocalDateTime joinedAt = LocalDateTime.now().minusHours(1);
        Participant participant = Participant.builder()
                .sessionId("session-1")
                .userId(8L)
                .role("EDITOR")
                .joinedAt(joinedAt)
                .cursorLine(12)
                .cursorCol(7)
                .build();

        participant.onCreate();

        assertThat(participant.getJoinedAt()).isEqualTo(joinedAt);
        assertThat(participant.getCursorLine()).isEqualTo(12);
        assertThat(participant.getCursorCol()).isEqualTo(7);
    }
}
