package com.app.codesync.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    private Long recipientId;
    private Long actorId;
    private String type;
    private String title;
    private String message;
    private String relatedId;
    private String relatedType;
}
