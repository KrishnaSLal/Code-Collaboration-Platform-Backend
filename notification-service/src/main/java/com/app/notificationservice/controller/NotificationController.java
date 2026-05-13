package com.app.notificationservice.controller;

import com.app.notificationservice.dto.*;
import com.app.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public NotificationResponse send(@RequestBody SendNotificationRequest request) {
        return notificationService.send(request);
    }

    @PostMapping("/bulk")
    public List<NotificationResponse> sendBulk(@RequestBody BulkNotificationRequest request) {
        return notificationService.sendBulk(request);
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable Long notificationId) {
        return notificationService.markAsRead(notificationId);
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    public List<NotificationResponse> markAllRead(@PathVariable Long recipientId) {
        return notificationService.markAllRead(recipientId);
    }

    @DeleteMapping("/recipient/{recipientId}/delete-read")
    public String deleteRead(@PathVariable Long recipientId) {
        return notificationService.deleteRead(recipientId);
    }

    @GetMapping("/recipient/{recipientId}")
    public List<NotificationResponse> getByRecipient(@PathVariable Long recipientId) {
        return notificationService.getByRecipient(recipientId);
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    public UnreadCountResponse getUnreadCount(@PathVariable Long recipientId) {
        return notificationService.getUnreadCount(recipientId);
    }

    @DeleteMapping("/{notificationId}")
    public String deleteNotification(@PathVariable Long notificationId) {
        return notificationService.deleteNotification(notificationId);
    }

    @PostMapping("/email")
    public String sendEmail(@RequestBody EmailNotificationRequest request) {
        return notificationService.sendEmail(request);
    }

    @GetMapping
    public List<NotificationResponse> getAll() {
        return notificationService.getAll();
    }
}