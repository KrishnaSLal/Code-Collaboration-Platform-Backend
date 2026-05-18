package com.app.collabservice.controller;

import com.app.collabservice.dto.CollabSessionResponse;
import com.app.collabservice.dto.CreateSessionRequest;
import com.app.collabservice.dto.CursorUpdateRequest;
import com.app.collabservice.dto.JoinSessionRequest;
import com.app.collabservice.dto.KickParticipantRequest;
import com.app.collabservice.dto.LeaveSessionRequest;
import com.app.collabservice.dto.ParticipantResponse;
import com.app.collabservice.dto.SessionInviteRequest;
import com.app.collabservice.service.CollabService;
import com.app.collabservice.websocket.CollabWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollabControllerTest {

    @Mock
    private CollabService collabService;

    @Mock
    private CollabWebSocketHandler collabWebSocketHandler;

    private CollabController controller;

    @BeforeEach
    void setUp() {
        controller = new CollabController(collabService, collabWebSocketHandler);
    }

    @Test
    void delegatesCollaborationOperations() {
        CreateSessionRequest createRequest = CreateSessionRequest.builder().projectId(1L).build();
        SessionInviteRequest inviteRequest = SessionInviteRequest.builder().recipientId(8L).actorId(3L).build();
        JoinSessionRequest joinRequest = JoinSessionRequest.builder().userId(8L).build();
        LeaveSessionRequest leaveRequest = LeaveSessionRequest.builder().userId(8L).build();
        CursorUpdateRequest cursorRequest = CursorUpdateRequest.builder().userId(8L).cursorLine(4).cursorCol(2).build();
        KickParticipantRequest kickRequest = KickParticipantRequest.builder().ownerId(3L).participantUserId(8L).build();
        CollabSessionResponse session = CollabSessionResponse.builder().sessionId("session-1").build();
        ParticipantResponse participant = ParticipantResponse.builder().userId(8L).build();

        when(collabService.createSession(createRequest, "Bearer token")).thenReturn(session);
        when(collabService.getSessionById("session-1")).thenReturn(session);
        when(collabService.getSessionsByProject(1L)).thenReturn(List.of(session));
        when(collabService.joinSession("session-1", joinRequest)).thenReturn(participant);
        when(collabService.getParticipants("session-1")).thenReturn(List.of(participant));
        when(collabService.updateCursor("session-1", cursorRequest)).thenReturn(participant);
        when(collabService.kickParticipant("session-1", kickRequest)).thenReturn(participant);
        when(collabService.getActiveSession(1L)).thenReturn(session);

        assertThat(controller.createSession(createRequest, "Bearer token")).isSameAs(session);
        assertThat(controller.getSessionById("session-1")).isSameAs(session);
        assertThat(controller.getSessionsByProject(1L)).containsExactly(session);
        assertThat(controller.sendSessionInvite("session-1", inviteRequest)).isEqualTo("Invite notification queued successfully");
        assertThat(controller.joinSession("session-1", joinRequest)).isSameAs(participant);
        assertThat(controller.leaveSession("session-1", leaveRequest)).isEqualTo("Participant left session successfully");
        assertThat(controller.endSession("session-1")).isEqualTo("Session ended successfully");
        assertThat(controller.getParticipants("session-1")).containsExactly(participant);
        assertThat(controller.updateCursor("session-1", cursorRequest)).isSameAs(participant);
        assertThat(controller.kickParticipant("session-1", kickRequest)).isSameAs(participant);
        assertThat(controller.getActiveSession(1L)).isSameAs(session);

        verify(collabService).sendSessionInvite("session-1", inviteRequest);
        verify(collabService).leaveSession("session-1", leaveRequest);
        verify(collabService).endSession("session-1");
        verify(collabWebSocketHandler).disconnectParticipant("session-1", 8L);
    }
}
