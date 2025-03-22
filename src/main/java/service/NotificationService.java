package com.divya.linkedinclone.service;

import com.divya.linkedinclone.entity.Notification;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.exception.UserNotFoundException;
import com.divya.linkedinclone.repository.NotificationRepository;
import com.divya.linkedinclone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.divya.linkedinclone.dto.NotificationResponse;
import java.util.stream.Collectors;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // Create a new notification
    public Notification createNotification(Long recipientId, Long senderId, String message) {
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new UserNotFoundException("Recipient user not found with id: " + recipientId));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender user not found with id: " + senderId));

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setSender(sender);
        notification.setMessage(message);
        return notificationRepository.save(notification);
    }

    // Get all unread notifications for a user
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndStatus(user, Notification.NotificationStatus.UNREAD);
        return unreadNotifications.stream()
                .map(notification -> new NotificationResponse(
                        notification.getId(),
                        notification.getSender().getId(),
                        notification.getSender().getName(),
                        notification.getMessage(),
                        notification.getStatus().toString(),
                        notification.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // Get all notifications for a user
    public List<NotificationResponse> getAllNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        List<Notification> allNotifications = notificationRepository.findByRecipient(user);
        return allNotifications.stream()
                .map(notification -> new NotificationResponse(
                        notification.getId(),
                        notification.getSender().getId(),
                        notification.getSender().getName(),
                        notification.getMessage(),
                        notification.getStatus().toString(),
                        notification.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // Mark a notification as read
    public void markNotificationAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
        notification.setStatus(Notification.NotificationStatus.READ);
        notificationRepository.save(notification);
    }
}