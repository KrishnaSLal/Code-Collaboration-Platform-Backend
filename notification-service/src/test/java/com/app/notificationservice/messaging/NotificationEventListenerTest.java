package com.app.notificationservice.messaging;

import com.app.codesync.messaging.NotificationEvent;
import com.app.notificationservice.dto.SendNotificationRequest;
import com.app.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void handleNotificationEventIgnoresNullEvent() {
        listener.handleNotificationEvent(null);

        verify(notificationService, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handleNotificationEventMapsEventToSendNotificationRequest() {
        NotificationEvent event = NotificationEvent.builder()
                .recipientId(1L)
                .actorId(2L)
                .type("SESSION_INVITE")
                .title("Join session")
                .message("Please join")
                .relatedId("session-1")
                .relatedType("SESSION")
                .build();

        listener.handleNotificationEvent(event);

        ArgumentCaptor<SendNotificationRequest> captor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(notificationService).send(captor.capture());

        SendNotificationRequest request = captor.getValue();
        assertThat(request.getRecipientId()).isEqualTo(1L);
        assertThat(request.getActorId()).isEqualTo(2L);
        assertThat(request.getType()).isEqualTo("SESSION_INVITE");
        assertThat(request.getRelatedId()).isEqualTo("session-1");
    }
}
