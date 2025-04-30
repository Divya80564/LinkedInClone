package com.divya.linkedinclone.controller;

import com.divya.linkedinclone.dto.ResumeResponse;
import com.divya.linkedinclone.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.divya.linkedinclone.dto.ParsedResumeData;
import com.divya.linkedinclone.dto.ResumeScoreResponse;
import com.divya.linkedinclone.dto.ResumeSuggestionResponse;
import com.divya.linkedinclone.service.ResumeAnalysisService;
import java.util.Map;
import com.divya.linkedinclone.dto.SkillGapResponse;  // Add this import
import com.divya.linkedinclone.exception.UserNotFoundException;  // Add this import
import com.divya.linkedinclone.exception.NoResumeContentException;  // Add this import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.divya.linkedinclone.dto.ParsedResumeResponse;
import org.springframework.http.HttpStatus;
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

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

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

    @GetMapping("/parsed")
    public ResponseEntity<?> getParsedResume(@PathVariable Long userId) {
        try {
            String parsedContent = userService.parseResumeContent(userId);
            return ResponseEntity.ok(new ParsedResumeResponse(userId, parsedContent));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Update ResumeController.java

    @PostMapping("/parse")
    public ResponseEntity<?> parseResume(@PathVariable Long userId) {
        try {
            ParsedResumeData parsedData = userService.parseUserResume(userId);
            return ResponseEntity.ok(parsedData);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Add to .\controller\ResumeController.java
    @GetMapping("/score")
    public ResponseEntity<?> scoreResume(@PathVariable Long userId) {
        try {
            int score = userService.scoreResume(userId);
            return ResponseEntity.ok(new ResumeScoreResponse(
                    userId,
                    score,
                    "Resume scored successfully",
                    userService.getScoreBreakdown(userId) // Optional detailed breakdown
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // .\controller\ResumeController.java (add this method)
    @GetMapping("/suggestions")
    public ResponseEntity<?> getResumeSuggestions(@PathVariable Long userId) {
        try {
            ResumeSuggestionResponse response = resumeAnalysisService.analyzeResume(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/skill-gap")
    public ResponseEntity<?> analyzeSkillGap(
            @PathVariable Long userId,
            @RequestParam(required = true) String role
    ) {
        try {
            SkillGapResponse response = resumeAnalysisService.analyzeSkillGap(userId, role);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (NoResumeContentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}