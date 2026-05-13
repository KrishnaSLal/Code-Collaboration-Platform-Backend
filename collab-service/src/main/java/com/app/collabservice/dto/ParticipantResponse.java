package com.app.collabservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantResponse {
    private Long participantId;
    private String sessionId;
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Integer cursorLine;
    private Integer cursorCol;
    private String color;
}
