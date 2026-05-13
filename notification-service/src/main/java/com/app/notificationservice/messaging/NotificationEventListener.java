package com.app.notificationservice.messaging;

import com.app.codesync.messaging.NotificationEvent;
import com.app.notificationservice.dto.SendNotificationRequest;
import com.app.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${codesync.rabbitmq.notifications.queue:codesync.notifications.queue}")
    public void handleNotificationEvent(NotificationEvent event) {
        if (event == null) {
            return;
        }

        notificationService.send(SendNotificationRequest.builder()
                .recipientId(event.getRecipientId())
                .actorId(event.getActorId())
                .type(event.getType())
                .title(event.getTitle())
                .message(event.getMessage())
                .relatedId(event.getRelatedId())
                .relatedType(event.getRelatedType())
                .build());
    }
}
