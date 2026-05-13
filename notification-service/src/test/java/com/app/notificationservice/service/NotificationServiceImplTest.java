package com.app.notificationservice.service;

import com.app.notificationservice.dto.BulkNotificationRequest;
import com.app.notificationservice.dto.EmailNotificationRequest;
import com.app.notificationservice.dto.NotificationResponse;
import com.app.notificationservice.dto.SendNotificationRequest;
import com.app.notificationservice.dto.UnreadCountResponse;
import com.app.notificationservice.entity.Notification;
import com.app.notificationservice.exception.NotificationNotFoundException;
import com.app.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void sendCreatesUnreadNotification() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId(1L)
                .actorId(2L)
                .type("SESSION_INVITE")
                .title("Join session")
                .message("Please join")
                .relatedId("session-1")
                .relatedType("SESSION")
                .build();

        when(repository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(10L);
            return notification;
        });

        NotificationResponse response = notificationService.send(request);

        assertThat(response.getNotificationId()).isEqualTo(10L);
        assertThat(response.getRecipientId()).isEqualTo(1L);
        assertThat(response.getType()).isEqualTo("SESSION_INVITE");
        assertThat(response.getIsRead()).isFalse();
    }

    @Test
    void sendBulkCreatesNotificationForEachRecipient() {
        BulkNotificationRequest request = BulkNotificationRequest.builder()
                .recipientIds(List.of(1L, 2L, 3L))
                .actorId(9L)
                .type("PROJECT_UPDATE")
                .title("Project updated")
                .message("Files changed")
                .relatedId("project-1")
                .relatedType("PROJECT")
                .build();

        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<NotificationResponse> responses = notificationService.sendBulk(request);

        assertThat(responses).hasSize(3);
        assertThat(responses).extracting(NotificationResponse::getRecipientId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void markAllReadUpdatesUnreadNotifications() {
        List<Notification> unread = new ArrayList<>(List.of(
                notification(1L, 10L, false),
                notification(2L, 10L, false)
        ));

        when(repository.findByRecipientIdAndIsRead(10L, false)).thenReturn(unread);
        when(repository.saveAll(unread)).thenReturn(unread);

        List<NotificationResponse> responses = notificationService.markAllRead(10L);

        assertThat(responses).hasSize(2);
        assertThat(responses).allMatch(NotificationResponse::getIsRead);
    }

    @Test
    void markAsReadUpdatesNotification() {
        Notification notification = notification(1L, 10L, false);
        when(repository.findByNotificationId(1L)).thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(1L);

        assertThat(response.getIsRead()).isTrue();
    }

    @Test
    void deleteReadDeletesReadNotificationsForRecipient() {
        String response = notificationService.deleteRead(10L);

        assertThat(response).isEqualTo("Read notifications deleted successfully");
        verify(repository).deleteByRecipientIdAndIsRead(10L, true);
    }

    @Test
    void getByRecipientReturnsRecipientNotifications() {
        when(repository.findByRecipientId(10L)).thenReturn(List.of(notification(1L, 10L, false)));

        List<NotificationResponse> responses = notificationService.getByRecipient(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getRecipientId()).isEqualTo(10L);
    }

    @Test
    void getUnreadCountReturnsRepositoryCount() {
        when(repository.countByRecipientIdAndIsRead(10L, false)).thenReturn(4L);

        UnreadCountResponse response = notificationService.getUnreadCount(10L);

        assertThat(response.getRecipientId()).isEqualTo(10L);
        assertThat(response.getUnreadCount()).isEqualTo(4L);
    }

    @Test
    void sendEmailSendsSimpleMailMessage() {
        EmailNotificationRequest request = EmailNotificationRequest.builder()
                .to("krishna@example.com")
                .subject("Hello")
                .body("Welcome to CodeSync")
                .build();

        String response = notificationService.sendEmail(request);

        assertThat(response).isEqualTo("Email sent successfully");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("krishna@example.com");
        assertThat(captor.getValue().getSubject()).isEqualTo("Hello");
        assertThat(captor.getValue().getText()).isEqualTo("Welcome to CodeSync");
    }

    @Test
    void deleteNotificationDeletesExistingNotification() {
        when(repository.findByNotificationId(1L)).thenReturn(Optional.of(notification(1L, 10L, false)));

        String response = notificationService.deleteNotification(1L);

        assertThat(response).isEqualTo("Notification deleted successfully");
        verify(repository).deleteByNotificationId(1L);
    }

    @Test
    void getAllReturnsAllNotifications() {
        when(repository.findAll()).thenReturn(List.of(
                notification(1L, 10L, false),
                notification(2L, 11L, true)
        ));

        List<NotificationResponse> responses = notificationService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(NotificationResponse::getNotificationId)
                .containsExactly(1L, 2L);
    }

    @Test
    void deleteNotificationThrowsWhenMissing() {
        when(repository.findByNotificationId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(404L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("Notification not found with id: 404");
    }

    private Notification notification(Long notificationId, Long recipientId, boolean isRead) {
        return Notification.builder()
                .notificationId(notificationId)
                .recipientId(recipientId)
                .actorId(99L)
                .type("INFO")
                .title("Title")
                .message("Message")
                .relatedId("related")
                .relatedType("TYPE")
                .isRead(isRead)
                .build();
    }
}
