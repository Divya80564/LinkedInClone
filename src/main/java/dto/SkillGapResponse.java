package com.divya.linkedinclone.dto;

import java.util.List;  // Add this import
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SkillGapResponse {
    private Long userId;
    private String targetRole;
    private List<String> missingSkills;  // Now this will work
    private String message;

    public SkillGapResponse(Long userId, String targetRole, List<String> missingSkills, String message) {
        this.userId = userId;
        this.targetRole = targetRole;
        this.missingSkills = missingSkills;
        this.message = message;
    }
}