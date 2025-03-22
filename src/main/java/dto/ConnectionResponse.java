package com.divya.linkedinclone.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConnectionResponse {
    private Long senderId;
    private String senderName;
    private String status;

    public ConnectionResponse(Long senderId, String senderName, String status) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.status = status;
    }
}