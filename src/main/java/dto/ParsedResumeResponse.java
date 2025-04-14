package com.divya.linkedinclone.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParsedResumeResponse {
    private Long userId;
    private String parsedContent;

    public ParsedResumeResponse(Long userId, String parsedContent) {
        this.userId = userId;
        this.parsedContent = parsedContent;
    }
}