package com.divya.linkedinclone.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String message;
    private String status;
    private LocalDateTime createdAt;

    public NotificationResponse(Long id, Long senderId, String senderName, String message, String status, LocalDateTime createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }
}