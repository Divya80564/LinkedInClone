package com.divya.linkedinclone.repository;

import com.divya.linkedinclone.entity.Notification;
import com.divya.linkedinclone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientAndStatus(User recipient, Notification.NotificationStatus status);
    List<Notification> findByRecipient(User recipient);
}