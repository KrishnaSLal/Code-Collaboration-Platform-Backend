package com.app.notificationservice.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkNotificationRequest {
    private List<Long> recipientIds;
    private Long actorId;
    private String type;
    private String title;
    private String message;
    private String relatedId;
    private String relatedType;
}