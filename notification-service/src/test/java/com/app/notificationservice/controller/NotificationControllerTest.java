package com.app.notificationservice.controller;

import com.app.notificationservice.dto.BulkNotificationRequest;
import com.app.notificationservice.dto.EmailNotificationRequest;
import com.app.notificationservice.dto.NotificationResponse;
import com.app.notificationservice.dto.SendNotificationRequest;
import com.app.notificationservice.dto.UnreadCountResponse;
import com.app.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
    }

    @Test
    void delegatesNotificationOperations() {
        SendNotificationRequest sendRequest = SendNotificationRequest.builder().recipientId(1L).build();
        BulkNotificationRequest bulkRequest = BulkNotificationRequest.builder().recipientIds(List.of(1L)).build();
        EmailNotificationRequest emailRequest = EmailNotificationRequest.builder().to("krishna@example.com").build();
        NotificationResponse response = NotificationResponse.builder().notificationId(1L).build();
        UnreadCountResponse unreadCount = UnreadCountResponse.builder().recipientId(1L).unreadCount(2L).build();

        when(notificationService.send(sendRequest)).thenReturn(response);
        when(notificationService.sendBulk(bulkRequest)).thenReturn(List.of(response));
        when(notificationService.markAsRead(1L)).thenReturn(response);
        when(notificationService.markAllRead(1L)).thenReturn(List.of(response));
        when(notificationService.deleteRead(1L)).thenReturn("deleted read");
        when(notificationService.getByRecipient(1L)).thenReturn(List.of(response));
        when(notificationService.getUnreadCount(1L)).thenReturn(unreadCount);
        when(notificationService.deleteNotification(1L)).thenReturn("deleted");
        when(notificationService.sendEmail(emailRequest)).thenReturn("sent");
        when(notificationService.getAll()).thenReturn(List.of(response));

        assertThat(controller.send(sendRequest)).isSameAs(response);
        assertThat(controller.sendBulk(bulkRequest)).containsExactly(response);
        assertThat(controller.markAsRead(1L)).isSameAs(response);
        assertThat(controller.markAllRead(1L)).containsExactly(response);
        assertThat(controller.deleteRead(1L)).isEqualTo("deleted read");
        assertThat(controller.getByRecipient(1L)).containsExactly(response);
        assertThat(controller.getUnreadCount(1L)).isSameAs(unreadCount);
        assertThat(controller.deleteNotification(1L)).isEqualTo("deleted");
        assertThat(controller.sendEmail(emailRequest)).isEqualTo("sent");
        assertThat(controller.getAll()).containsExactly(response);
    }
}
