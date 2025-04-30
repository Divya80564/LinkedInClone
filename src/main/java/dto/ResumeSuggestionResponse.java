// .\dto\ResumeSuggestionResponse.java
package com.divya.linkedinclone.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ResumeSuggestionResponse {
    private Long userId;
    private int score;
    private List<String> suggestions;
    private List<String> missingSections;
    private List<String> strengths;
    private LocalDateTime analysisDate;
    private String resumeVersion;

    public ResumeSuggestionResponse(Long userId, int score, List<String> suggestions,
                                    List<String> missingSections, List<String> strengths) {
        this.userId = userId;
        this.score = score;
        this.suggestions = suggestions;
        this.missingSections = missingSections;
        this.strengths = strengths;
        this.analysisDate = LocalDateTime.now();
        this.resumeVersion = "1.0";
    }
}