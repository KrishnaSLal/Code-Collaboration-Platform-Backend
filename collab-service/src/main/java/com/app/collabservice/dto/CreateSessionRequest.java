package com.app.collabservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSessionRequest {
    private Long projectId;
    private Long fileId;
    private Long ownerId;
    private String language;
    private Integer maxParticipants;
    private Boolean passwordProtected;
    private String sessionPassword;
}