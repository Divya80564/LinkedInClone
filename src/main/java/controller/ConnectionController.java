package com.divya.linkedinclone.controller;

import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.service.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.divya.linkedinclone.entity.Connection;
import java.util.List;
import java.util.Map;
import com.divya.linkedinclone.dto.ConnectionResponse;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    @Autowired
    private ConnectionService connectionService;

    @PostMapping("/request")
    public ResponseEntity<?> sendConnectionRequest(@RequestBody Map<String, Long> request) {
        Long senderId = request.get("senderId");
        Long receiverId = request.get("receiverId");
        String message = connectionService.sendConnectionRequest(senderId, receiverId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptConnectionRequest(@RequestBody Map<String, Long> request) {
        Long receiverId = request.get("receiverId");
        Long senderId = request.get("senderId");
        String message = connectionService.acceptConnectionRequest(receiverId, senderId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/reject")
    public ResponseEntity<?> rejectConnectionRequest(@RequestBody Map<String, Long> request) {
        Long receiverId = request.get("receiverId");
        Long senderId = request.get("senderId");
        String message = connectionService.rejectConnectionRequest(receiverId, senderId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @DeleteMapping("/cancel")
    public ResponseEntity<?> cancelConnectionRequest(@RequestBody Map<String, Long> request) {
        Long senderId = request.get("senderId");
        Long receiverId = request.get("receiverId");
        String message = connectionService.cancelConnectionRequest(senderId, receiverId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeConnection(@RequestBody Map<String, Long> request) {
        Long userId = request.get("userId");
        Long connectionId = request.get("connectionId");
        String message = connectionService.removeConnection(userId, connectionId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests(@RequestParam Long userId) {
        List<ConnectionResponse> pendingRequests = connectionService.getPendingRequests(userId);
        return ResponseEntity.ok(Map.of("pendingRequests", pendingRequests));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllConnections(@RequestParam Long userId) {
        List<ConnectionResponse> connections = connectionService.getAllConnections(userId);
        return ResponseEntity.ok(Map.of("connections", connections));
    }

    @GetMapping("/mutual")
    public ResponseEntity<?> getMutualConnections(@RequestParam Long userId, @RequestParam Long otherUserId) {
        List<ConnectionResponse> mutualConnections = connectionService.getMutualConnections(userId, otherUserId);
        return ResponseEntity.ok(Map.of("mutualConnections", mutualConnections));
    }
}