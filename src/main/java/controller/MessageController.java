package com.divya.linkedinclone.controller;

import com.divya.linkedinclone.entity.Message; // Add this import
import com.divya.linkedinclone.dto.MessageResponse;
import com.divya.linkedinclone.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> request) {
        Long senderId = Long.valueOf(request.get("senderId").toString());
        Long receiverId = Long.valueOf(request.get("receiverId").toString());
        String message = request.get("message").toString(); // Changed from "content" to "message"

        // Call the service method and store the result in a variable
        Message sentMessage = messageService.sendMessage(senderId, receiverId, message);

        // Convert the Message entity to a MessageResponse DTO
        MessageResponse response = new MessageResponse(
                sentMessage.getId(),
                sentMessage.getSender().getId(),
                sentMessage.getReceiver().getId(),
                sentMessage.getMessage(), // Changed from "getContent" to "getMessage"
                sentMessage.getSentAt()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Message sent successfully",
                "messageId", response.getId() // Use the correct variable name
        ));
    }

    @GetMapping("/conversation/{userId1}/{userId2}")
    public ResponseEntity<List<MessageResponse>> getConversation(@PathVariable Long userId1, @PathVariable Long userId2) {
        List<MessageResponse> messages = messageService.getConversation(userId1, userId2);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<MessageResponse>> getAllConversations(@PathVariable Long userId) {
        List<MessageResponse> conversations = messageService.getAllConversations(userId);
        return ResponseEntity.ok(conversations);
    }
}