package com.app.notificationservice.service;

import com.app.notificationservice.dto.*;

import java.util.List;

public interface NotificationService {

    NotificationResponse send(SendNotificationRequest request);

    List<NotificationResponse> sendBulk(BulkNotificationRequest request);

    NotificationResponse markAsRead(Long notificationId);

    List<NotificationResponse> markAllRead(Long recipientId);

    String deleteRead(Long recipientId);

    List<NotificationResponse> getByRecipient(Long recipientId);

    UnreadCountResponse getUnreadCount(Long recipientId);

    String deleteNotification(Long notificationId);

    String sendEmail(EmailNotificationRequest request);

    List<NotificationResponse> getAll();
}