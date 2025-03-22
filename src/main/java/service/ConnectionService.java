package com.divya.linkedinclone.service;

import com.divya.linkedinclone.entity.Connection;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.exception.UserNotFoundException;
import com.divya.linkedinclone.repository.ConnectionRepository;
import com.divya.linkedinclone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Optional;
import com.divya.linkedinclone.dto.ConnectionResponse;

@Service
public class ConnectionService {

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService; // Add this line


    public String sendConnectionRequest(Long senderId, Long receiverId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender user not found with id: " + senderId));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new UserNotFoundException("Receiver user not found with id: " + receiverId));

        Optional<Connection> existingConnection = connectionRepository.findBySenderAndReceiver(sender, receiver);
        if (existingConnection.isPresent()) {
            return "Connection request already sent.";
        }

        Connection connection = new Connection();
        connection.setSender(sender);
        connection.setReceiver(receiver);
        connection.setStatus(Connection.ConnectionStatus.PENDING);
        connectionRepository.save(connection);

        // Create a notification for the receiver
        notificationService.createNotification(receiverId, senderId, "You have a new connection request from " + sender.getName());

        return "Connection request sent successfully.";
    }

    public String acceptConnectionRequest(Long receiverId, Long senderId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new UserNotFoundException("Receiver user not found with id: " + receiverId));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender user not found with id: " + senderId));

        Connection connection = connectionRepository.findBySenderAndReceiver(sender, receiver)
                .orElseThrow(() -> new RuntimeException("Connection request not found."));

        connection.setStatus(Connection.ConnectionStatus.CONNECTED);
        connectionRepository.save(connection);

        // Create a notification for the sender
        notificationService.createNotification(senderId, receiverId, receiver.getName() + " accepted your connection request.");

        return "Connection request accepted. You are now connected!";
    }

    public String rejectConnectionRequest(Long receiverId, Long senderId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new UserNotFoundException("Receiver user not found with id: " + receiverId));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender user not found with id: " + senderId));

        Connection connection = connectionRepository.findBySenderAndReceiver(sender, receiver)
                .orElseThrow(() -> new RuntimeException("Connection request not found."));

        connection.setStatus(Connection.ConnectionStatus.REJECTED);
        connectionRepository.save(connection);

        // Create a notification for the sender
        notificationService.createNotification(senderId, receiverId, receiver.getName() + " rejected your connection request.");

        return "Connection request rejected.";
    }

    public String cancelConnectionRequest(Long senderId, Long receiverId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender user not found with id: " + senderId));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new UserNotFoundException("Receiver user not found with id: " + receiverId));

        Connection connection = connectionRepository.findBySenderAndReceiver(sender, receiver)
                .orElseThrow(() -> new RuntimeException("Connection request not found."));

        connectionRepository.delete(connection);

        return "Connection request canceled.";
    }

    public String removeConnection(Long userId, Long connectionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        User connectionUser = userRepository.findById(connectionId)
                .orElseThrow(() -> new UserNotFoundException("Connection user not found with id: " + connectionId));

        // Find the connection between the two users
        Optional<Connection> connection = connectionRepository.findBySenderAndReceiver(user, connectionUser);
        if (connection.isEmpty()) {
            connection = connectionRepository.findBySenderAndReceiver(connectionUser, user);
        }

        if (connection.isEmpty()) {
            throw new RuntimeException("Connection not found.");
        }

        connectionRepository.delete(connection.get());
        return "Connection removed successfully.";
    }

    public List<ConnectionResponse> getPendingRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        List<Connection> pendingConnections = connectionRepository.findByReceiverAndStatus(user, Connection.ConnectionStatus.PENDING);
        return pendingConnections.stream()
                .map(connection -> new ConnectionResponse(
                        connection.getSender().getId(),
                        connection.getSender().getName(),
                        connection.getStatus().toString()
                ))
                .collect(Collectors.toList());
    }

    public List<ConnectionResponse> getMutualConnections(Long userId, Long otherUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new UserNotFoundException("Other user not found with id: " + otherUserId));

        List<Connection> userConnections = connectionRepository.findBySenderOrReceiver(user, user);
        List<Connection> otherUserConnections = connectionRepository.findBySenderOrReceiver(otherUser, otherUser);

        // Find mutual connections
        return userConnections.stream()
                .filter(connection -> otherUserConnections.contains(connection))
                .map(connection -> {
                    User connectedUser = connection.getSender().equals(user) ? connection.getReceiver() : connection.getSender();
                    return new ConnectionResponse(connectedUser.getId(), connectedUser.getName(), connection.getStatus().toString());
                })
                .collect(Collectors.toList());
    }

    public List<ConnectionResponse> getAllConnections(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        List<Connection> connections = connectionRepository.findBySenderOrReceiver(user, user);
        return connections.stream()
                .map(connection -> {
                    User connectedUser = connection.getSender().equals(user) ? connection.getReceiver() : connection.getSender();
                    return new ConnectionResponse(connectedUser.getId(), connectedUser.getName(), connection.getStatus().toString());
                })
                .collect(Collectors.toList());
    }
}