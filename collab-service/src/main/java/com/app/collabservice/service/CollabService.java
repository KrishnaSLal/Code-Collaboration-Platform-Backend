package com.app.collabservice.service;

import com.app.collabservice.dto.*;

import java.util.List;

public interface CollabService {

    CollabSessionResponse createSession(CreateSessionRequest request, String authorizationHeader);

    CollabSessionResponse getSessionById(String sessionId);

    List<CollabSessionResponse> getSessionsByProject(Long projectId);

    void sendSessionInvite(String sessionId, SessionInviteRequest request);

    ParticipantResponse joinSession(String sessionId, JoinSessionRequest request);

    void leaveSession(String sessionId, LeaveSessionRequest request);

    void endSession(String sessionId);

    List<ParticipantResponse> getParticipants(String sessionId);

    ParticipantResponse updateCursor(String sessionId, CursorUpdateRequest request);

    ParticipantResponse kickParticipant(String sessionId, KickParticipantRequest request);

    CollabSessionResponse getActiveSession(Long projectId);
}
