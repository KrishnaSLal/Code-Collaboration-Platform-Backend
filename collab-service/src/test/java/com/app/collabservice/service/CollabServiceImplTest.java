package com.app.collabservice.service;

import com.app.collabservice.dto.CollabSessionResponse;
import com.app.collabservice.dto.CreateSessionRequest;
import com.app.collabservice.dto.CursorUpdateRequest;
import com.app.collabservice.dto.JoinSessionRequest;
import com.app.collabservice.dto.KickParticipantRequest;
import com.app.collabservice.dto.LeaveSessionRequest;
import com.app.collabservice.dto.ParticipantResponse;
import com.app.collabservice.dto.SessionInviteRequest;
import com.app.collabservice.dto.UserSummaryResponse;
import com.app.collabservice.entity.CollabSession;
import com.app.collabservice.entity.Participant;
import com.app.collabservice.exception.ParticipantNotFoundException;
import com.app.collabservice.exception.SessionNotFoundException;
import com.app.collabservice.messaging.NotificationEventPublisher;
import com.app.collabservice.repository.CollabSessionRepository;
import com.app.collabservice.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollabServiceImplTest {

    @Mock
    private CollabSessionRepository sessionRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private AuthUserClient authUserClient;

    @InjectMocks
    private CollabServiceImpl collabService;

    @Test
    void createSessionCreatesHostParticipant() {
        CreateSessionRequest request = CreateSessionRequest.builder()
                .projectId(1L)
                .fileId(2L)
                .ownerId(3L)
                .language("Java")
                .maxParticipants(5)
                .passwordProtected(false)
                .build();

        when(sessionRepository.save(any(CollabSession.class))).thenAnswer(invocation -> {
            CollabSession session = invocation.getArgument(0);
            session.setSessionId("session-1");
            return session;
        });
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CollabSessionResponse response = collabService.createSession(request);

        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getOwnerId()).isEqualTo(3L);

        ArgumentCaptor<Participant> participantCaptor = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepository).save(participantCaptor.capture());
        assertThat(participantCaptor.getValue().getSessionId()).isEqualTo("session-1");
        assertThat(participantCaptor.getValue().getUserId()).isEqualTo(3L);
        assertThat(participantCaptor.getValue().getRole()).isEqualTo("HOST");
        assertThat(participantCaptor.getValue().getColor()).isEqualTo("#FF5733");
    }

    @Test
    void sendSessionInvitePublishesWhenSessionIsActive() {
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session("session-1", "ACTIVE")));
        SessionInviteRequest request = SessionInviteRequest.builder()
                .recipientId(10L)
                .actorId(3L)
                .build();

        collabService.sendSessionInvite("session-1", request);

        verify(notificationEventPublisher).publishSessionInvite("session-1", request);
    }

    @Test
    void getSessionByIdReturnsMappedSession() {
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session("session-1", "ACTIVE")));

        CollabSessionResponse response = collabService.getSessionById("session-1");

        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getSessionsByProjectReturnsMappedSessions() {
        when(sessionRepository.findByProjectId(1L)).thenReturn(List.of(session("session-1", "ACTIVE")));

        List<CollabSessionResponse> responses = collabService.getSessionsByProject(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getProjectId()).isEqualTo(1L);
    }

    @Test
    void sendSessionInviteRejectsEndedSession() {
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session("session-1", "ENDED")));

        assertThatThrownBy(() -> collabService.sendSessionInvite("session-1", SessionInviteRequest.builder()
                .recipientId(10L)
                .actorId(3L)
                .build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cannot invite users to an ended session");
    }

    @Test
    void joinSessionCreatesParticipantWhenCapacityAllows() {
        CollabSession session = session("session-1", "ACTIVE");
        session.setMaxParticipants(3);
        session.setPasswordProtected(false);

        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionIdAndUserId("session-1", 8L)).thenReturn(Optional.empty());
        when(participantRepository.countBySessionIdAndLeftAtIsNull("session-1")).thenReturn(1L);
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
            Participant participant = invocation.getArgument(0);
            participant.setParticipantId(99L);
            return participant;
        });

        ParticipantResponse response = collabService.joinSession("session-1", JoinSessionRequest.builder()
                .userId(8L)
                .role("EDITOR")
                .build());

        assertThat(response.getParticipantId()).isEqualTo(99L);
        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(response.getUserId()).isEqualTo(8L);
        assertThat(response.getRole()).isEqualTo("EDITOR");
        assertThat(response.getColor()).isEqualTo("#33FF57");
    }

    @Test
    void joinSessionReturnsExistingActiveParticipantWithoutSaving() {
        CollabSession session = session("session-1", "ACTIVE");
        Participant participant = participant(10L, "session-1", 8L);

        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionIdAndUserId("session-1", 8L)).thenReturn(Optional.of(participant));

        ParticipantResponse response = collabService.joinSession("session-1", JoinSessionRequest.builder()
                .userId(8L)
                .role("VIEWER")
                .build());

        assertThat(response.getParticipantId()).isEqualTo(10L);
        assertThat(response.getRole()).isEqualTo("EDITOR");
    }

    @Test
    void joinSessionReactivatesParticipantWhoPreviouslyLeft() {
        CollabSession session = session("session-1", "ACTIVE");
        Participant participant = participant(10L, "session-1", 8L);
        participant.setLeftAt(java.time.LocalDateTime.now().minusMinutes(5));

        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionIdAndUserId("session-1", 8L)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParticipantResponse response = collabService.joinSession("session-1", JoinSessionRequest.builder()
                .userId(8L)
                .role("VIEWER")
                .build());

        assertThat(response.getLeftAt()).isNull();
        assertThat(response.getRole()).isEqualTo("VIEWER");
    }

    @Test
    void joinSessionRejectsWhenParticipantLimitReached() {
        CollabSession session = session("session-1", "ACTIVE");
        session.setMaxParticipants(1);

        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionIdAndUserId("session-1", 8L)).thenReturn(Optional.empty());
        when(participantRepository.countBySessionIdAndLeftAtIsNull("session-1")).thenReturn(1L);

        assertThatThrownBy(() -> collabService.joinSession("session-1", JoinSessionRequest.builder()
                .userId(8L)
                .role("EDITOR")
                .build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Maximum participant limit reached");
    }

    @Test
    void joinSessionRejectsWrongPassword() {
        CollabSession session = session("session-1", "ACTIVE");
        session.setPasswordProtected(true);
        session.setSessionPassword("secret");
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> collabService.joinSession("session-1", JoinSessionRequest.builder()
                .userId(8L)
                .role("EDITOR")
                .sessionPassword("wrong")
                .build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid session password");
    }

    @Test
    void leaveSessionMarksParticipantAsLeft() {
        Participant participant = participant(10L, "session-1", 8L);
        when(participantRepository.findBySessionIdAndUserId("session-1", 8L)).thenReturn(Optional.of(participant));

        collabService.leaveSession("session-1", LeaveSessionRequest.builder()
                .userId(8L)
                .build());

        assertThat(participant.getLeftAt()).isNotNull();
        verify(participantRepository).save(participant);
    }

    @Test
    void endSessionMarksSessionEnded() {
        CollabSession session = session("session-1", "ACTIVE");
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));

        collabService.endSession("session-1");

        assertThat(session.getStatus()).isEqualTo("ENDED");
        assertThat(session.getEndedAt()).isNotNull();
        verify(sessionRepository).save(session);
    }

    @Test
    void updateCursorChangesParticipantPosition() {
        Participant participant = participant(10L, "session-1", 8L);
        when(participantRepository.findBySessionIdAndUserId("session-1", 8L)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParticipantResponse response = collabService.updateCursor("session-1", CursorUpdateRequest.builder()
                .userId(8L)
                .cursorLine(20)
                .cursorCol(5)
                .build());

        assertThat(response.getCursorLine()).isEqualTo(20);
        assertThat(response.getCursorCol()).isEqualTo(5);
    }

    @Test
    void kickParticipantRequiresSessionOwner() {
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session("session-1", "ACTIVE")));

        assertThatThrownBy(() -> collabService.kickParticipant("session-1", KickParticipantRequest.builder()
                .ownerId(99L)
                .participantUserId(8L)
                .build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Only session owner can kick participants");
    }

    @Test
    void kickParticipantMarksParticipantAsLeftWhenOwnerRequestsIt() {
        CollabSession session = session("session-1", "ACTIVE");
        Participant participant = participant(10L, "session-1", 8L);

        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionIdAndUserId("session-1", 8L)).thenReturn(Optional.of(participant));
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParticipantResponse response = collabService.kickParticipant("session-1", KickParticipantRequest.builder()
                .ownerId(3L)
                .participantUserId(8L)
                .build());

        assertThat(response.getLeftAt()).isNotNull();
    }

    @Test
    void kickParticipantRejectsKickingSessionOwner() {
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session("session-1", "ACTIVE")));

        assertThatThrownBy(() -> collabService.kickParticipant("session-1", KickParticipantRequest.builder()
                .ownerId(3L)
                .participantUserId(3L)
                .build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Session owner cannot kick themselves");
    }

    @Test
    void getParticipantsFiltersParticipantsWhoLeft() {
        Participant active = participant(10L, "session-1", 8L);
        Participant left = participant(11L, "session-1", 9L);
        left.setLeftAt(java.time.LocalDateTime.now());

        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session("session-1", "ACTIVE")));
        when(participantRepository.findBySessionId("session-1")).thenReturn(List.of(active, left));

        List<ParticipantResponse> responses = collabService.getParticipants("session-1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUserId()).isEqualTo(8L);
    }

    @Test
    void getParticipantsIncludesRegisteredNamesForAllActiveUsers() {
        Participant host = participant(10L, "session-1", 3L);
        host.setRole("HOST");
        Participant editor = participant(11L, "session-1", 8L);

        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(session("session-1", "ACTIVE")));
        when(participantRepository.findBySessionId("session-1")).thenReturn(List.of(host, editor));
        when(authUserClient.getUsersByIds(any())).thenReturn(Map.of(
                3L, UserSummaryResponse.builder()
                        .userId(3L)
                        .username("Krishna")
                        .fullName("Krishna")
                        .email("krishna@example.com")
                        .build(),
                8L, UserSummaryResponse.builder()
                        .userId(8L)
                        .username("Shweta")
                        .fullName("Shweta")
                        .email("shweta@example.com")
                        .build()
        ));

        List<ParticipantResponse> responses = collabService.getParticipants("session-1");

        assertThat(responses).extracting(ParticipantResponse::getFullName)
                .containsExactly("Krishna", "Shweta");
        assertThat(responses).extracting(ParticipantResponse::getUsername)
                .containsExactly("Krishna", "Shweta");
        assertThat(responses).extracting(ParticipantResponse::getEmail)
                .containsExactly("krishna@example.com", "shweta@example.com");
    }

    @Test
    void getActiveSessionThrowsWhenNoActiveSessionExists() {
        when(sessionRepository.findByProjectIdAndStatus(1L, "ACTIVE")).thenReturn(List.of());

        assertThatThrownBy(() -> collabService.getActiveSession(1L))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessage("No active session found for project: 1");
    }

    @Test
    void getActiveSessionReturnsFirstActiveSession() {
        when(sessionRepository.findByProjectIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(session("session-1", "ACTIVE"), session("session-2", "ACTIVE")));

        CollabSessionResponse response = collabService.getActiveSession(1L);

        assertThat(response.getSessionId()).isEqualTo("session-1");
    }

    @Test
    void leaveSessionThrowsWhenParticipantMissing() {
        when(participantRepository.findBySessionIdAndUserId("session-1", 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collabService.leaveSession("session-1", LeaveSessionRequest.builder()
                .userId(8L)
                .build()))
                .isInstanceOf(ParticipantNotFoundException.class)
                .hasMessage("Participant not found in session");
    }

    private CollabSession session(String sessionId, String status) {
        return CollabSession.builder()
                .sessionId(sessionId)
                .projectId(1L)
                .fileId(2L)
                .ownerId(3L)
                .status(status)
                .language("Java")
                .maxParticipants(5)
                .passwordProtected(false)
                .build();
    }

    private Participant participant(Long id, String sessionId, Long userId) {
        return Participant.builder()
                .participantId(id)
                .sessionId(sessionId)
                .userId(userId)
                .role("EDITOR")
                .cursorLine(1)
                .cursorCol(1)
                .color("#FF5733")
                .build();
    }
}
