package com.app.notificationservice.repository;

import com.app.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientId(Long recipientId);

    List<Notification> findByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    long countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    List<Notification> findByType(String type);

    List<Notification> findByRelatedId(String relatedId);

    Optional<Notification> findByNotificationId(Long notificationId);

    void deleteByNotificationId(Long notificationId);

    void deleteByRecipientIdAndIsRead(Long recipientId, Boolean isRead);
}