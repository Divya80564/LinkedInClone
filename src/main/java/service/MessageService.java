package com.divya.linkedinclone.service;

import com.divya.linkedinclone.entity.Message;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.exception.UserNotFoundException;
import com.divya.linkedinclone.repository.MessageRepository;
import com.divya.linkedinclone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.divya.linkedinclone.dto.MessageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    public Message sendMessage(Long senderId, Long receiverId, String message) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender not found with id: " + senderId));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new UserNotFoundException("Receiver not found with id: " + receiverId));

        Message newMessage = new Message();
        newMessage.setSender(sender);
        newMessage.setReceiver(receiver);
        newMessage.setMessage(message); // Ensure this is correctly defined
        newMessage.setSentAt(LocalDateTime.now());

        return messageRepository.save(newMessage); // Return the Message entity
    }

    public List<MessageResponse> getConversation(Long userId1, Long userId2) {
        List<Message> messages = messageRepository.findBySenderIdAndReceiverId(userId1, userId2);
        messages.addAll(messageRepository.findBySenderIdAndReceiverId(userId2, userId1));

        return messages.stream()
                .map(message -> new MessageResponse(
                        message.getId(),
                        message.getSender().getId(),
                        message.getReceiver().getId(),
                        message.getMessage(), // Changed from "getContent" to "getMessage"
                        message.getSentAt()
                ))
                .collect(Collectors.toList());
    }

    public List<MessageResponse> getAllConversations(Long userId) {
        List<Message> messages = messageRepository.findBySenderIdOrReceiverId(userId, userId);

        return messages.stream()
                .map(message -> new MessageResponse(
                        message.getId(),
                        message.getSender().getId(),
                        message.getReceiver().getId(),
                        message.getMessage(), // Changed from "getContent" to "getMessage"
                        message.getSentAt()
                ))
                .collect(Collectors.toList());
    }
}