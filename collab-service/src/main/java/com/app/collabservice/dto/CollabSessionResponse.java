package com.app.collabservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabSessionResponse {
    private String sessionId;
    private Long projectId;
    private Long fileId;
    private Long ownerId;
    private String status;
    private String language;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
    private Integer maxParticipants;
    private Boolean passwordProtected;
}