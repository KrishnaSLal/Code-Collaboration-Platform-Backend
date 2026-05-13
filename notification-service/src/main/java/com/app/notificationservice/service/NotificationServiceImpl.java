package com.app.notificationservice.service;

import com.app.notificationservice.dto.*;
import com.app.notificationservice.entity.Notification;
import com.app.notificationservice.exception.NotificationNotFoundException;
import com.app.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final JavaMailSender mailSender;

    @Override
    public NotificationResponse send(SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .actorId(request.getActorId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .isRead(false)
                .build();

        return mapToResponse(repository.save(notification));
    }

    @Override
    public List<NotificationResponse> sendBulk(BulkNotificationRequest request) {
        return request.getRecipientIds()
                .stream()
                .map(recipientId -> {
                    Notification notification = Notification.builder()
                            .recipientId(recipientId)
                            .actorId(request.getActorId())
                            .type(request.getType())
                            .title(request.getTitle())
                            .message(request.getMessage())
                            .relatedId(request.getRelatedId())
                            .relatedType(request.getRelatedType())
                            .isRead(false)
                            .build();

                    return mapToResponse(repository.save(notification));
                })
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = fetchNotification(notificationId);
        notification.setIsRead(true);
        return mapToResponse(repository.save(notification));
    }

    @Override
    public List<NotificationResponse> markAllRead(Long recipientId) {
        List<Notification> notifications = repository.findByRecipientIdAndIsRead(recipientId, false);
        notifications.forEach(notification -> notification.setIsRead(true));
        return repository.saveAll(notifications)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public String deleteRead(Long recipientId) {
        repository.deleteByRecipientIdAndIsRead(recipientId, true);
        return "Read notifications deleted successfully";
    }

    @Override
    public List<NotificationResponse> getByRecipient(Long recipientId) {
        return repository.findByRecipientId(recipientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UnreadCountResponse getUnreadCount(Long recipientId) {
        long unreadCount = repository.countByRecipientIdAndIsRead(recipientId, false);
        return UnreadCountResponse.builder()
                .recipientId(recipientId)
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    public String deleteNotification(Long notificationId) {
        fetchNotification(notificationId);
        repository.deleteByNotificationId(notificationId);
        return "Notification deleted successfully";
    }

    @Override
    public String sendEmail(EmailNotificationRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject(request.getSubject());
        message.setText(request.getBody());
        mailSender.send(message);
        return "Email sent successfully";
    }

    @Override
    public List<NotificationResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Notification fetchNotification(Long notificationId) {
        return repository.findByNotificationId(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found with id: " + notificationId));
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .recipientId(notification.getRecipientId())
                .actorId(notification.getActorId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}