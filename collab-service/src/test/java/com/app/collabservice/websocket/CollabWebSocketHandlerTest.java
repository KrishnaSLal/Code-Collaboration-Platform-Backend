package com.app.collabservice.websocket;

import com.app.collabservice.dto.CollabSocketMessage;
import com.app.collabservice.dto.ParticipantResponse;
import com.app.collabservice.service.CollabService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollabWebSocketHandlerTest {

    @Mock
    private CollabService collabService;

    private ObjectMapper objectMapper;
    private CollabWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        handler = new CollabWebSocketHandler(objectMapper, collabService);
    }

    @Test
    void connectionStoresAttributesAndBroadcastsParticipant() throws Exception {
        when(collabService.getParticipants("session-1")).thenReturn(List.of(ParticipantResponse.builder()
                .participantId(10L)
                .sessionId("session-1")
                .userId(42L)
                .fullName("Krishna")
                .build()));
        WebSocketSession session = session("ws://localhost/collab?sessionId=session-1&userId=42&clientId=client-a");

        handler.afterConnectionEstablished(session);

        assertThat(session.getAttributes())
                .containsEntry("sessionId", "session-1")
                .containsEntry("userId", "42")
                .containsEntry("clientId", "client-a");

        CollabSocketMessage message = sentMessage(session);
        assertThat(message.getType()).isEqualTo("USER_CONNECTED");
        assertThat(message.getUserId()).isEqualTo(42L);
        assertThat(message.getParticipant().getFullName()).isEqualTo("Krishna");
        assertThat(message.getSentAt()).isNotNull();
    }

    @Test
    void connectionWithoutSessionIdIsRejected() throws Exception {
        WebSocketSession session = session("ws://localhost/collab?userId=42");

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status ->
                status.getCode() == CloseStatus.BAD_DATA.getCode()
                        && "sessionId is required".equals(status.getReason())));
        verifyNoInteractions(collabService);
    }

    @Test
    void connectionWithNullUriIsRejectedWithoutParsingQueryParams() throws Exception {
        WebSocketSession session = org.mockito.Mockito.mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(null);

        handler.afterConnectionEstablished(session);

        verify(session).close(argThat(status -> status.getCode() == CloseStatus.BAD_DATA.getCode()));
    }

    @Test
    void invalidUserIdConnectsWithoutParticipantLookup() throws Exception {
        WebSocketSession session = session("ws://localhost/collab?sessionId=session-1&userId=not-number&clientId=client-a");

        handler.afterConnectionEstablished(session);

        CollabSocketMessage message = sentMessage(session);
        assertThat(message.getUserId()).isNull();
        assertThat(message.getParticipant()).isNull();
        verifyNoInteractions(collabService);
    }

    @Test
    void cursorUpdateUsesSessionAttributesAndBroadcastsToOpenSockets() throws Exception {
        WebSocketSession sender = session("ws://localhost/collab?sessionId=session-1&userId=42&clientId=client-a");
        WebSocketSession other = session("ws://localhost/collab?sessionId=session-1&userId=99&clientId=client-b");
        handler.afterConnectionEstablished(sender);
        handler.afterConnectionEstablished(other);
        clearInvocations(sender, other);

        when(collabService.updateCursor("session-1", com.app.collabservice.dto.CursorUpdateRequest.builder()
                .userId(42L)
                .cursorLine(12)
                .cursorCol(4)
                .build())).thenReturn(ParticipantResponse.builder()
                .sessionId("session-1")
                .userId(42L)
                .cursorLine(12)
                .cursorCol(4)
                .build());

        handler.handleTextMessage(sender, new TextMessage("""
                {"type":"CURSOR_UPDATE","userId":42,"cursorLine":12,"cursorCol":4}
                """));

        CollabSocketMessage senderMessage = sentMessage(sender);
        CollabSocketMessage otherMessage = sentMessage(other);
        assertThat(senderMessage.getSessionId()).isEqualTo("session-1");
        assertThat(senderMessage.getClientId()).isEqualTo("client-a");
        assertThat(senderMessage.getParticipant().getCursorLine()).isEqualTo(12);
        assertThat(otherMessage.getType()).isEqualTo("CURSOR_UPDATE");
    }

    @Test
    void contentChangeIsNotEchoedToSender() throws Exception {
        WebSocketSession sender = session("ws://localhost/collab?sessionId=session-1&userId=42&clientId=client-a");
        WebSocketSession other = session("ws://localhost/collab?sessionId=session-1&userId=99&clientId=client-b");
        handler.afterConnectionEstablished(sender);
        handler.afterConnectionEstablished(other);
        clearInvocations(sender, other);

        handler.handleTextMessage(sender, new TextMessage("""
                {"type":"CONTENT_CHANGE","sessionId":"session-1","clientId":"client-a","content":"class Main {}"}
                """));

        verify(sender, never()).sendMessage(any());
        CollabSocketMessage message = sentMessage(other);
        assertThat(message.getType()).isEqualTo("CONTENT_CHANGE");
        assertThat(message.getContent()).isEqualTo("class Main {}");
    }

    @Test
    void connectionClosedRemovesSocketAndNotifiesRemainingParticipants() throws Exception {
        WebSocketSession closing = session("ws://localhost/collab?sessionId=session-1&userId=42&clientId=client-a");
        WebSocketSession remaining = session("ws://localhost/collab?sessionId=session-1&userId=99&clientId=client-b");
        handler.afterConnectionEstablished(closing);
        handler.afterConnectionEstablished(remaining);
        clearInvocations(closing, remaining);

        handler.afterConnectionClosed(closing, CloseStatus.NORMAL);

        verify(closing, never()).sendMessage(any());
        CollabSocketMessage message = sentMessage(remaining);
        assertThat(message.getType()).isEqualTo("USER_DISCONNECTED");
        assertThat(message.getUserId()).isEqualTo(42L);
        assertThat(message.getClientId()).isEqualTo("client-a");
    }

    @Test
    void connectionClosedWithoutStoredSessionDoesNothing() throws Exception {
        WebSocketSession session = session("ws://localhost/collab?sessionId=session-1&userId=42&clientId=client-a");

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(session, never()).sendMessage(any());
    }

    @Test
    void disconnectParticipantReturnsWhenSessionHasNoSockets() {
        handler.disconnectParticipant("missing-session", 42L);

        verifyNoInteractions(collabService);
    }

    @Test
    void disconnectParticipantClosesOnlyMatchingOpenSockets() throws Exception {
        WebSocketSession matching = session("ws://localhost/collab?sessionId=session-1&userId=42&clientId=client-a");
        WebSocketSession other = session("ws://localhost/collab?sessionId=session-1&userId=99&clientId=client-b");
        handler.afterConnectionEstablished(matching);
        handler.afterConnectionEstablished(other);

        handler.disconnectParticipant("session-1", 42L);

        verify(matching).close(argThat(status ->
                status.getCode() == CloseStatus.POLICY_VIOLATION.getCode()
                        && "Kicked from collaboration session".equals(status.getReason())));
        verify(other, never()).close(any());
    }

    @Test
    void disconnectParticipantIgnoresCloseFailures() throws Exception {
        WebSocketSession matching = session("ws://localhost/collab?sessionId=session-1&userId=42&clientId=client-a");
        handler.afterConnectionEstablished(matching);
        doThrow(new IOException("already gone")).when(matching).close(any(CloseStatus.class));

        handler.disconnectParticipant("session-1", 42L);

        verify(matching).close(any(CloseStatus.class));
    }

    @Test
    void broadcastIgnoresSendFailures() throws Exception {
        WebSocketSession failingSession = session("ws://localhost/collab?sessionId=session-1&userId=42&clientId=client-a");
        doThrow(new IOException("network closed")).when(failingSession).sendMessage(any());

        handler.afterConnectionEstablished(failingSession);

        verify(failingSession).sendMessage(any());
    }

    private WebSocketSession session(String uri) {
        WebSocketSession session = org.mockito.Mockito.mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        lenient().when(session.getUri()).thenReturn(URI.create(uri));
        lenient().when(session.getAttributes()).thenReturn(attributes);
        lenient().when(session.isOpen()).thenReturn(true);
        return session;
    }

    private CollabSocketMessage sentMessage(WebSocketSession session) throws IOException {
        ArgumentCaptor<WebSocketMessage<?>> captor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(session).sendMessage(captor.capture());
        return objectMapper.readValue(((TextMessage) captor.getValue()).getPayload(), CollabSocketMessage.class);
    }
}
