package com.app.collabservice.websocket;

import com.app.collabservice.dto.CollabSocketMessage;
import com.app.collabservice.dto.CursorUpdateRequest;
import com.app.collabservice.dto.ParticipantResponse;
import com.app.collabservice.service.CollabService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class CollabWebSocketHandler extends TextWebSocketHandler {

    private static final String SESSION_ID_ATTR = "sessionId";
    private static final String USER_ID_ATTR = "userId";
    private static final String CLIENT_ID_ATTR = "clientId";

    private final ObjectMapper objectMapper;
    private final CollabService collabService;
    private final Map<String, Set<WebSocketSession>> sessionsByCollabSession = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> params = queryParams(session.getUri());
        String sessionId = params.get("sessionId");
        String userId = params.get("userId");
        String clientId = params.get("clientId");

        if (sessionId == null || sessionId.isBlank()) {
            session.close(CloseStatus.BAD_DATA.withReason("sessionId is required"));
            return;
        }

        session.getAttributes().put(SESSION_ID_ATTR, sessionId);
        session.getAttributes().put(USER_ID_ATTR, userId);
        session.getAttributes().put(CLIENT_ID_ATTR, clientId);
        sessionsByCollabSession.computeIfAbsent(sessionId, ignored -> ConcurrentHashMap.newKeySet()).add(session);

        Long parsedUserId = parseLong(userId);
        broadcast(sessionId, CollabSocketMessage.builder()
                .type("USER_CONNECTED")
                .sessionId(sessionId)
                .clientId(clientId)
                .userId(parsedUserId)
                .participant(findParticipant(sessionId, parsedUserId))
                .sentAt(LocalDateTime.now())
                .build(), null);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        CollabSocketMessage message = objectMapper.readValue(textMessage.getPayload(), CollabSocketMessage.class);
        String sessionId = message.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = (String) session.getAttributes().get(SESSION_ID_ATTR);
            message.setSessionId(sessionId);
        }

        message.setSentAt(LocalDateTime.now());
        if (message.getClientId() == null || message.getClientId().isBlank()) {
            message.setClientId((String) session.getAttributes().get(CLIENT_ID_ATTR));
        }

        if ("CURSOR_UPDATE".equals(message.getType()) && message.getUserId() != null) {
            message.setParticipant(collabService.updateCursor(sessionId, CursorUpdateRequest.builder()
                    .userId(message.getUserId())
                    .cursorLine(message.getCursorLine())
                    .cursorCol(message.getCursorCol())
                    .build()));
        }

        WebSocketSession excludedSession = "CONTENT_CHANGE".equals(message.getType()) ? session : null;
        broadcast(sessionId, message, excludedSession);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = (String) session.getAttributes().get(SESSION_ID_ATTR);
        if (sessionId == null) {
            return;
        }

        Set<WebSocketSession> connectedSessions = sessionsByCollabSession.get(sessionId);
        if (connectedSessions != null) {
            connectedSessions.remove(session);
            if (connectedSessions.isEmpty()) {
                sessionsByCollabSession.remove(sessionId);
            }
        }

        Long parsedUserId = parseLong((String) session.getAttributes().get(USER_ID_ATTR));
        if (!hasOpenConnection(sessionId, parsedUserId)) {
            clearParticipantCursor(sessionId, parsedUserId);
        }

        broadcast(sessionId, CollabSocketMessage.builder()
                .type("USER_DISCONNECTED")
                .sessionId(sessionId)
                .clientId((String) session.getAttributes().get(CLIENT_ID_ATTR))
                .userId(parsedUserId)
                .sentAt(LocalDateTime.now())
                .build(), null);
    }

    public void disconnectParticipant(String sessionId, Long userId) {
        Set<WebSocketSession> connectedSessions = sessionsByCollabSession.get(sessionId);
        if (connectedSessions == null || connectedSessions.isEmpty()) {
            return;
        }

        connectedSessions.stream()
                .filter(WebSocketSession::isOpen)
                .filter(session -> userId.equals(parseLong((String) session.getAttributes().get(USER_ID_ATTR))))
                .forEach(session -> {
                    try {
                        session.close(CloseStatus.POLICY_VIOLATION.withReason("Kicked from collaboration session"));
                    } catch (IOException ignored) {
                        // The close callback will clean up sockets that make it that far.
                    }
                });
    }

    private void broadcast(String sessionId, CollabSocketMessage message, WebSocketSession excludedSession) {
        Set<WebSocketSession> connectedSessions = sessionsByCollabSession.get(sessionId);
        if (connectedSessions == null || connectedSessions.isEmpty()) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(message);
            for (WebSocketSession connectedSession : connectedSessions) {
                if (!connectedSession.isOpen() || connectedSession.equals(excludedSession)) {
                    continue;
                }
                sendMessage(connectedSession, payload);
            }
        } catch (IOException ignored) {
            // Individual stale sockets are cleaned up on close; failed broadcasts should not break edits.
        }
    }

    private void sendMessage(WebSocketSession session, String payload) {
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException ignored) {
            // Individual stale sockets are cleaned up on close; failed broadcasts should not break edits.
        }
    }

    private boolean hasOpenConnection(String sessionId, Long userId) {
        if (userId == null) {
            return false;
        }

        Set<WebSocketSession> connectedSessions = sessionsByCollabSession.get(sessionId);
        if (connectedSessions == null || connectedSessions.isEmpty()) {
            return false;
        }

        return connectedSessions.stream()
                .filter(WebSocketSession::isOpen)
                .anyMatch(connectedSession ->
                        userId.equals(parseLong((String) connectedSession.getAttributes().get(USER_ID_ATTR))));
    }

    private void clearParticipantCursor(String sessionId, Long userId) {
        if (userId == null) {
            return;
        }

        try {
            collabService.updateCursor(sessionId, CursorUpdateRequest.builder()
                    .userId(userId)
                    .cursorLine(null)
                    .cursorCol(null)
                    .build());
        } catch (RuntimeException ignored) {
            // Socket disconnect should not fail the session cleanup path.
        }
    }

    private Map<String, String> queryParams(URI uri) {
        if (uri == null) {
            return Map.of();
        }
        Map<String, String> params = new ConcurrentHashMap<>();
        UriComponentsBuilder.fromUri(uri).build().getQueryParams()
                .forEach((key, values) -> {
                    if (!values.isEmpty()) {
                        params.put(key, values.get(0));
                    }
                });
        return params;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private ParticipantResponse findParticipant(String sessionId, Long userId) {
        if (userId == null) {
            return null;
        }

        try {
            return collabService.getParticipants(sessionId)
                    .stream()
                    .filter(participant -> userId.equals(participant.getUserId()))
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
