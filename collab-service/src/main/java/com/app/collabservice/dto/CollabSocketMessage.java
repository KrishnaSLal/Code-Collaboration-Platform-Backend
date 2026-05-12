package com.app.collabservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollabSocketMessage {
    private String type;
    private String sessionId;
    private String clientId;
    private Long userId;
    private Long fileId;
    private String content;
    private Integer cursorLine;
    private Integer cursorCol;
    private ParticipantResponse participant;
    private LocalDateTime sentAt;
}
