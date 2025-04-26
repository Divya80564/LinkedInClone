// .\dto\ParsedResumeData.java
package com.divya.linkedinclone.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParsedResumeData {
    private String name;
    private String email;
    private String phone;
    private String skills;
    private String education;
    private String experience;
    private String summary;
}