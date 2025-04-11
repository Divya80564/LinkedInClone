package com.divya.linkedinclone.controller;

import com.divya.linkedinclone.dto.ResumeResponse;
import com.divya.linkedinclone.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
@RestController
@RequestMapping("/api/users/{userId}/resume")
public class ResumeController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<?> uploadResume(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            userService.uploadResume(userId, file);
            return ResponseEntity.ok(new ResumeResponse(
                    "Resume uploaded successfully",
                    userService.getUserById(userId).getResumePath(),
                    originalFilename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getResume(@PathVariable Long userId) {
        try {
            Resource resource = userService.loadResumeAsResource(userId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve resume"));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteResume(@PathVariable Long userId) {
        try {
            userService.deleteResume(userId);
            return ResponseEntity.ok(Map.of("message", "Resume deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete resume"));
        }
    }
}