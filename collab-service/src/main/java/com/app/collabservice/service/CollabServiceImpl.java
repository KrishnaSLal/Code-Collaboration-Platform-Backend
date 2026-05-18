package com.app.collabservice.service;

import com.app.collabservice.dto.*;
import com.app.collabservice.entity.CollabSession;
import com.app.collabservice.entity.Participant;
import com.app.collabservice.exception.AuthenticationRequiredException;
import com.app.collabservice.exception.CollaborationAccessDeniedException;
import com.app.collabservice.exception.ParticipantNotFoundException;
import com.app.collabservice.exception.SessionNotFoundException;
import com.app.collabservice.messaging.NotificationEventPublisher;
import com.app.collabservice.repository.CollabSessionRepository;
import com.app.collabservice.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollabServiceImpl implements CollabService {

    private final CollabSessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final AuthUserClient authUserClient;
    private final ProjectClient projectClient;

    @Override
    public CollabSessionResponse createSession(CreateSessionRequest request, String authorizationHeader) {
        UserSummaryResponse currentUser = getAuthenticatedUser(authorizationHeader);
        ProjectResponse project = projectClient.getProjectById(request.getProjectId());
        if (!Objects.equals(project.getOwnerId(), currentUser.getUserId())) {
            throw new CollaborationAccessDeniedException("Only the project owner can start a collaboration session");
        }

        CollabSession session = CollabSession.builder()
                .projectId(request.getProjectId())
                .fileId(request.getFileId())
                .ownerId(currentUser.getUserId())
                .status("ACTIVE")
                .language(request.getLanguage())
                .maxParticipants(request.getMaxParticipants())
                .passwordProtected(request.getPasswordProtected())
                .sessionPassword(request.getSessionPassword())
                .build();

        CollabSession savedSession = sessionRepository.save(session);

        Participant ownerParticipant = Participant.builder()
                .sessionId(savedSession.getSessionId())
                .userId(currentUser.getUserId())
                .role("HOST")
                .color(generateColor(0))
                .build();

        participantRepository.save(ownerParticipant);

        return mapSession(savedSession);
    }

    @Override
    public CollabSessionResponse getSessionById(String sessionId) {
        return mapSession(fetchSession(sessionId));
    }

    @Override
    public List<CollabSessionResponse> getSessionsByProject(Long projectId) {
        ProjectResponse project = projectClient.getProjectById(projectId);
        return sessionRepository.findByProjectId(projectId)
                .stream()
                .filter(session -> isProjectOwnerSession(session, project))
                .map(this::mapSession)
                .collect(Collectors.toList());
    }

    @Override
    public void sendSessionInvite(String sessionId, SessionInviteRequest request) {
        CollabSession session = fetchSession(sessionId);
        ensureSessionStartedByProjectOwner(session);
        if (!"ACTIVE".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Cannot invite users to an ended session");
        }

        notificationEventPublisher.publishSessionInvite(session.getSessionId(), request);
    }

    @Override
    public ParticipantResponse joinSession(String sessionId, JoinSessionRequest request) {
        CollabSession session = fetchSession(sessionId);
        ensureSessionStartedByProjectOwner(session);

        if ("ENDED".equalsIgnoreCase(session.getStatus())) {
            throw new RuntimeException("Session already ended");
        }

        if (Boolean.TRUE.equals(session.getPasswordProtected())) {
            if (request.getSessionPassword() == null ||
                    !request.getSessionPassword().equals(session.getSessionPassword())) {
                throw new RuntimeException("Invalid session password");
            }
        }

        var existingParticipant = participantRepository.findBySessionIdAndUserId(sessionId, request.getUserId());
        if (existingParticipant.isEmpty()) {
            long participantCount = participantRepository.countBySessionIdAndLeftAtIsNull(sessionId);
            if (session.getMaxParticipants() != null && participantCount >= session.getMaxParticipants()) {
                throw new RuntimeException("Maximum participant limit reached");
            }
        }

        return existingParticipant
                .map(existing -> {
                    if (existing.getLeftAt() != null) {
                        existing.setLeftAt(null);
                        existing.setRole(request.getRole());
                        return mapParticipantWithRegisteredName(participantRepository.save(existing));
                    }
                    return mapParticipantWithRegisteredName(existing);
                })
                .orElseGet(() -> {
                    Participant participant = Participant.builder()
                            .sessionId(sessionId)
                            .userId(request.getUserId())
                            .role(request.getRole())
                            .color(generateColor((int) participantRepository.countBySessionIdAndLeftAtIsNull(sessionId)))
                            .build();

                    return mapParticipantWithRegisteredName(participantRepository.save(participant));
                });
    }

    @Override
    public void leaveSession(String sessionId, LeaveSessionRequest request) {
        Participant participant = participantRepository.findBySessionIdAndUserId(sessionId, request.getUserId())
                .orElseThrow(() -> new ParticipantNotFoundException("Participant not found in session"));

        participant.setLeftAt(LocalDateTime.now());
        participantRepository.save(participant);
    }

    @Override
    public void endSession(String sessionId) {
        CollabSession session = fetchSession(sessionId);
        ensureSessionStartedByProjectOwner(session);
        session.setStatus("ENDED");
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Override
    public List<ParticipantResponse> getParticipants(String sessionId) {
        ensureSessionStartedByProjectOwner(fetchSession(sessionId));
        List<Participant> activeParticipants = participantRepository.findBySessionId(sessionId)
                .stream()
                .filter(participant -> participant.getLeftAt() == null)
                .collect(Collectors.toList());

        return mapParticipantsWithRegisteredNames(activeParticipants);
    }

    @Override
    public ParticipantResponse updateCursor(String sessionId, CursorUpdateRequest request) {
        ensureSessionStartedByProjectOwner(fetchSession(sessionId));
        Participant participant = participantRepository.findBySessionIdAndUserId(sessionId, request.getUserId())
                .orElseThrow(() -> new ParticipantNotFoundException("Participant not found in session"));

        participant.setCursorLine(request.getCursorLine());
        participant.setCursorCol(request.getCursorCol());

        return mapParticipantWithRegisteredName(participantRepository.save(participant));
    }

    @Override
    public ParticipantResponse kickParticipant(String sessionId, KickParticipantRequest request) {
        CollabSession session = fetchSession(sessionId);
        ensureSessionStartedByProjectOwner(session);

        if (!session.getOwnerId().equals(request.getOwnerId())) {
            throw new RuntimeException("Only session owner can kick participants");
        }

        if (session.getOwnerId().equals(request.getParticipantUserId())) {
            throw new RuntimeException("Session owner cannot kick themselves");
        }

        Participant participant = participantRepository.findBySessionIdAndUserId(sessionId, request.getParticipantUserId())
                .orElseThrow(() -> new ParticipantNotFoundException("Participant not found in session"));

        participant.setLeftAt(LocalDateTime.now());
        return mapParticipantWithRegisteredName(participantRepository.save(participant));
    }

    @Override
    public CollabSessionResponse getActiveSession(Long projectId) {
        ProjectResponse project = projectClient.getProjectById(projectId);
        List<CollabSession> activeSessions = sessionRepository.findByProjectIdAndStatus(projectId, "ACTIVE");
        List<CollabSession> projectOwnerSessions = activeSessions.stream()
                .filter(session -> isProjectOwnerSession(session, project))
                .collect(Collectors.toList());
        if (projectOwnerSessions.isEmpty()) {
            throw new SessionNotFoundException("No active session found for project: " + projectId);
        }
        return mapSession(projectOwnerSessions.get(0));
    }

    private CollabSession fetchSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + sessionId));
    }

    private UserSummaryResponse getAuthenticatedUser(String authorizationHeader) {
        try {
            return authUserClient.getCurrentUser(authorizationHeader);
        } catch (RuntimeException exception) {
            throw new AuthenticationRequiredException("Authentication required to start a collaboration session");
        }
    }

    private void ensureSessionStartedByProjectOwner(CollabSession session) {
        ProjectResponse project = projectClient.getProjectById(session.getProjectId());
        if (!isProjectOwnerSession(session, project)) {
            throw new CollaborationAccessDeniedException("Only the project owner can start a collaboration session");
        }
    }

    private boolean isProjectOwnerSession(CollabSession session, ProjectResponse project) {
        return Objects.equals(session.getOwnerId(), project.getOwnerId());
    }

    private CollabSessionResponse mapSession(CollabSession session) {
        return CollabSessionResponse.builder()
                .sessionId(session.getSessionId())
                .projectId(session.getProjectId())
                .fileId(session.getFileId())
                .ownerId(session.getOwnerId())
                .status(session.getStatus())
                .language(session.getLanguage())
                .createdAt(session.getCreatedAt())
                .endedAt(session.getEndedAt())
                .maxParticipants(session.getMaxParticipants())
                .passwordProtected(session.getPasswordProtected())
                .build();
    }

    private ParticipantResponse mapParticipantWithRegisteredName(Participant participant) {
        Map<Long, UserSummaryResponse> usersById = authUserClient.getUsersByIds(List.of(participant.getUserId()));
        return mapParticipant(participant, usersById == null ? Map.of() : usersById);
    }

    private List<ParticipantResponse> mapParticipantsWithRegisteredNames(List<Participant> participants) {
        Map<Long, UserSummaryResponse> usersById = authUserClient.getUsersByIds(
                participants.stream().map(Participant::getUserId).collect(Collectors.toSet())
        );
        Map<Long, UserSummaryResponse> safeUsersById = usersById == null ? Map.of() : usersById;

        return participants.stream()
                .map(participant -> mapParticipant(participant, safeUsersById))
                .collect(Collectors.toList());
    }

    private ParticipantResponse mapParticipant(Participant participant, Map<Long, UserSummaryResponse> usersById) {
        UserSummaryResponse user = usersById.get(participant.getUserId());

        return ParticipantResponse.builder()
                .participantId(participant.getParticipantId())
                .sessionId(participant.getSessionId())
                .userId(participant.getUserId())
                .username(user == null ? null : resolveUsername(user))
                .fullName(user == null ? null : user.getFullName())
                .email(user == null ? null : user.getEmail())
                .role(participant.getRole())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .cursorLine(participant.getCursorLine())
                .cursorCol(participant.getCursorCol())
                .color(participant.getColor())
                .build();
    }

    private String resolveUsername(UserSummaryResponse user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }

        return user.getFullName();
    }

    private String generateColor(int index) {
        List<String> colors = Arrays.asList(
                "#FF5733", "#33FF57", "#3357FF", "#F39C12", "#8E44AD",
                "#1ABC9C", "#E74C3C", "#2ECC71", "#3498DB", "#9B59B6"
        );
        return colors.get(index % colors.size());
    }
}
