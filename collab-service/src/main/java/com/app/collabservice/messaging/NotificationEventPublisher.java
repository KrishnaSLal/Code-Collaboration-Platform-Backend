package com.app.collabservice.messaging;

import com.app.codesync.messaging.NotificationEvent;
import com.app.collabservice.dto.SessionInviteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${codesync.rabbitmq.notifications.exchange:codesync.notifications.exchange}")
    private String notificationExchange;

    @Value("${codesync.rabbitmq.notifications.routing-key:notification.session.invite}")
    private String notificationRoutingKey;

    public void publishSessionInvite(String sessionId, SessionInviteRequest request) {
        NotificationEvent event = NotificationEvent.builder()
                .recipientId(request.getRecipientId())
                .actorId(request.getActorId())
                .type("SESSION_INVITE")
                .title(resolveTitle(request))
                .message(resolveMessage(sessionId, request))
                .relatedId(sessionId)
                .relatedType("SESSION")
                .build();

        rabbitTemplate.convertAndSend(notificationExchange, notificationRoutingKey, event);
    }

    private String resolveTitle(SessionInviteRequest request) {
        return StringUtils.hasText(request.getTitle()) ? request.getTitle() : "Session invite";
    }

    private String resolveMessage(String sessionId, SessionInviteRequest request) {
        return StringUtils.hasText(request.getMessage())
                ? request.getMessage()
                : "You have been invited to collaboration session " + sessionId + ".";
    }
}
