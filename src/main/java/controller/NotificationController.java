package com.divya.linkedinclone.controller;

import com.divya.linkedinclone.entity.Notification;
import com.divya.linkedinclone.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import com.divya.linkedinclone.dto.NotificationResponse;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // Get all unread notifications for a user
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(@RequestParam Long userId) {
        List<NotificationResponse> unreadNotifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(Map.of("unreadNotifications", unreadNotifications));
    }

    // Get all notifications for a user
    @GetMapping("/all")
    public ResponseEntity<?> getAllNotifications(@RequestParam Long userId) {
        List<NotificationResponse> allNotifications = notificationService.getAllNotifications(userId);
        return ResponseEntity.ok(Map.of("allNotifications", allNotifications));
    }

    // Mark a notification as read
    @PostMapping("/mark-as-read/{notificationId}")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long notificationId) {
        notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read."));
    }
}