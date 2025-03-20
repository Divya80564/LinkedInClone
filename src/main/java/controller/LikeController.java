package com.divya.linkedinclone.controller;

import com.divya.linkedinclone.entity.Like;
import com.divya.linkedinclone.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.divya.linkedinclone.dto.LikeResponse;
import java.util.Map; // Import for Map
import com.divya.linkedinclone.exception.PostNotFoundException; // Import for PostNotFoundException
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @PostMapping("/{postId}")
    public ResponseEntity<LikeResponse> toggleLike(
            @PathVariable Long postId,
            @RequestParam Long userId
    ) {
        LikeResponse response = likeService.toggleLike(postId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getLikeCount(@PathVariable Long postId) {
        try {
            Long likeCount = likeService.getLikeCount(postId);
            return ResponseEntity.ok(Map.of("postId", postId, "likeCount", likeCount));
        } catch (PostNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}