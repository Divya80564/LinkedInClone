package com.divya.linkedinclone.controller;

import com.divya.linkedinclone.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/connections")
public class FollowController {

    @Autowired
    private FollowService followService;

    @PostMapping("/follow/{userId}")
    public ResponseEntity<?> followUser(@PathVariable Long userId, @RequestParam Long followerId) {
        String message = followService.followUser(followerId, userId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @DeleteMapping("/unfollow/{userId}")
    public ResponseEntity<?> unfollowUser(@PathVariable Long userId, @RequestParam Long followerId) {
        String message = followService.unfollowUser(followerId, userId);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/followers/{userId}")
    public ResponseEntity<?> getFollowers(@PathVariable Long userId) {
        List<Long> followers = followService.getFollowers(userId);
        return ResponseEntity.ok(Map.of("followers", followers));
    }

    @GetMapping("/following/{userId}")
    public ResponseEntity<?> getFollowing(@PathVariable Long userId) {
        List<Long> following = followService.getFollowing(userId);
        return ResponseEntity.ok(Map.of("following", following));
    }
}