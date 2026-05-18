package com.app.collabservice.controller;

import com.app.collabservice.dto.*;
import com.app.collabservice.service.*;
import com.app.collabservice.websocket.CollabWebSocketHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class CollabController {

    private final CollabService collabService;
    private final CollabWebSocketHandler collabWebSocketHandler;

    @PostMapping
    public CollabSessionResponse createSession(@RequestBody CreateSessionRequest request,
                                               @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return collabService.createSession(request, authorizationHeader);
    }

    @GetMapping("/{sessionId}")
    public CollabSessionResponse getSessionById(@PathVariable String sessionId) {
        return collabService.getSessionById(sessionId);
    }

    @GetMapping("/project/{projectId}")
    public List<CollabSessionResponse> getSessionsByProject(@PathVariable Long projectId) {
        return collabService.getSessionsByProject(projectId);
    }

    @PostMapping("/{sessionId}/invite")
    public String sendSessionInvite(@PathVariable String sessionId,
                                    @Valid @RequestBody SessionInviteRequest request) {
        collabService.sendSessionInvite(sessionId, request);
        return "Invite notification queued successfully";
    }

    @PostMapping("/{sessionId}/join")
    public ParticipantResponse joinSession(@PathVariable String sessionId,
                                           @RequestBody JoinSessionRequest request) {
        return collabService.joinSession(sessionId, request);
    }

    @PostMapping("/{sessionId}/leave")
    public String leaveSession(@PathVariable String sessionId,
                               @RequestBody LeaveSessionRequest request) {
        collabService.leaveSession(sessionId, request);
        return "Participant left session successfully";
    }

    @PostMapping("/{sessionId}/end")
    public String endSession(@PathVariable String sessionId) {
        collabService.endSession(sessionId);
        return "Session ended successfully";
    }

    @GetMapping("/{sessionId}/participants")
    public List<ParticipantResponse> getParticipants(@PathVariable String sessionId) {
        return collabService.getParticipants(sessionId);
    }

    @PutMapping("/{sessionId}/cursor")
    public ParticipantResponse updateCursor(@PathVariable String sessionId,
                                            @RequestBody CursorUpdateRequest request) {
        return collabService.updateCursor(sessionId, request);
    }

    @PostMapping("/{sessionId}/kick")
    public ParticipantResponse kickParticipant(@PathVariable String sessionId,
                                               @RequestBody KickParticipantRequest request) {
        ParticipantResponse participant = collabService.kickParticipant(sessionId, request);
        collabWebSocketHandler.disconnectParticipant(sessionId, request.getParticipantUserId());
        return participant;
    }

    @GetMapping("/project/{projectId}/active")
    public CollabSessionResponse getActiveSession(@PathVariable Long projectId) {
        return collabService.getActiveSession(projectId);
    }
}
