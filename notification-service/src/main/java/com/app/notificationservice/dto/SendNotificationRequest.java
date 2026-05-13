package com.app.notificationservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendNotificationRequest {
    private Long recipientId;
    private Long actorId;
    private String type;
    private String title;
    private String message;
    private String relatedId;
    private String relatedType;
}