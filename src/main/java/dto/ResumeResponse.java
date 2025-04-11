// Create new file: .\dto\ResumeResponse.java
package com.divya.linkedinclone.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeResponse {
    private String message;
    private String path;
    private String name;

    public ResumeResponse(String message, String path, String name) {
        this.message = message;
        this.path = path;
        this.name = name;
    }
}