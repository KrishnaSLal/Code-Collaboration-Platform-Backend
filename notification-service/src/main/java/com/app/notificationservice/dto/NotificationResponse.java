package com.app.notificationservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long notificationId;
    private Long recipientId;
    private Long actorId;
    private String type;
    private String title;
    private String message;
    private String relatedId;
    private String relatedType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}