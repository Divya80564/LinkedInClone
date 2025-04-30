// .\dto\ResumeScoreResponse.java
package com.divya.linkedinclone.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeScoreResponse {
    private Long userId;
    private int score;
    private String message;
    private String breakdown; // Optional: detailed breakdown of scoring

    public ResumeScoreResponse(Long userId, int score, String message) {
        this.userId = userId;
        this.score = score;
        this.message = message;
    }

    public ResumeScoreResponse(Long userId, int score, String message, String breakdown) {
        this.userId = userId;
        this.score = score;
        this.message = message;
        this.breakdown = breakdown;
    }
}