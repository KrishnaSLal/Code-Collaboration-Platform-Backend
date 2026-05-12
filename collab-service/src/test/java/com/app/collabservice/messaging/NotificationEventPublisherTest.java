package com.app.collabservice.messaging;

import com.app.codesync.messaging.NotificationEvent;
import com.app.collabservice.dto.SessionInviteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private NotificationEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new NotificationEventPublisher(rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "notificationExchange", "codesync.notifications.exchange");
        ReflectionTestUtils.setField(publisher, "notificationRoutingKey", "notification.session.invite");
    }

    @Test
    void publishSessionInviteSendsDefaultNotificationEventToConfiguredExchange() {
        SessionInviteRequest request = SessionInviteRequest.builder()
                .recipientId(10L)
                .actorId(3L)
                .build();

        publisher.publishSessionInvite("session-1", request);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq("codesync.notifications.exchange"),
                eq("notification.session.invite"),
                eventCaptor.capture()
        );

        NotificationEvent event = eventCaptor.getValue();
        assertThat(event.getRecipientId()).isEqualTo(10L);
        assertThat(event.getActorId()).isEqualTo(3L);
        assertThat(event.getType()).isEqualTo("SESSION_INVITE");
        assertThat(event.getTitle()).isEqualTo("Session invite");
        assertThat(event.getMessage()).isEqualTo("You have been invited to collaboration session session-1.");
        assertThat(event.getRelatedId()).isEqualTo("session-1");
        assertThat(event.getRelatedType()).isEqualTo("SESSION");
    }

    @Test
    void publishSessionInviteUsesCustomTitleAndMessageWhenProvided() {
        SessionInviteRequest request = SessionInviteRequest.builder()
                .recipientId(10L)
                .actorId(3L)
                .title("Pair programming")
                .message("Join me now")
                .build();

        publisher.publishSessionInvite("session-1", request);

        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq("codesync.notifications.exchange"),
                eq("notification.session.invite"),
                eventCaptor.capture()
        );

        assertThat(eventCaptor.getValue().getTitle()).isEqualTo("Pair programming");
        assertThat(eventCaptor.getValue().getMessage()).isEqualTo("Join me now");
    }
}
