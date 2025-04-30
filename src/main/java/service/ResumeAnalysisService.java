// .\service\ResumeAnalysisService.java
package com.divya.linkedinclone.service;

import com.divya.linkedinclone.dto.ResumeSuggestionResponse;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.exception.UserNotFoundException;
import com.divya.linkedinclone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.divya.linkedinclone.exception.NoResumeContentException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeAnalysisService {

    @Autowired
    private UserRepository userRepository;

    private static final List<String> KEY_SECTIONS = Arrays.asList(
            "skills", "experience", "education", "projects"
    );

    private static final List<String> INDUSTRY_KEYWORDS = Arrays.asList(
            "java", "spring", "aws", "docker", "kubernetes",
            "react", "angular", "node", "python", "machine learning",
            "sql", "nosql", "rest", "graphql", "microservices"
    );

    private static final Map<String, String> RELATED_TERMS = new HashMap<>();
    static {
        RELATED_TERMS.put("java", "spring");
        RELATED_TERMS.put("spring", "java");
        RELATED_TERMS.put("react", "javascript");
        RELATED_TERMS.put("aws", "cloud");
    }

    public ResumeSuggestionResponse analyzeResume(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.getParsedResumeText() == null || user.getParsedResumeText().isEmpty()) {
            throw new RuntimeException("No resume content available for analysis");
        }

        String resumeText = user.getParsedResumeText().toLowerCase();
        List<String> missingSections = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> strengths = new ArrayList<>();

        // Check for missing sections
        for (String section : KEY_SECTIONS) {
            if (!resumeText.contains(section)) {
                missingSections.add(section);
                suggestions.add("Add a " + capitalize(section) + " section");
            }
        }

        // Analyze content quality
        analyzeContentQuality(resumeText, suggestions, strengths);

        // Check for industry keywords
        analyzeKeywords(resumeText, suggestions, strengths);

        // Calculate score
        int score = calculateScore(resumeText, missingSections.size());

        return new ResumeSuggestionResponse(
                userId,
                score,
                suggestions,
                missingSections,
                strengths
        );
    }

    private void analyzeContentQuality(String resumeText, List<String> suggestions, List<String> strengths) {
        // Check skills section
        if (resumeText.contains("skills")) {
            String skillsSection = extractSection(resumeText, "skills");
            int skillCount = skillsSection.split("[,•]").length;

            if (skillCount < 5) {
                suggestions.add("Consider adding more skills (currently " + skillCount + ")");
            } else {
                strengths.add("Good variety of skills (" + skillCount + ")");
            }
        }

        // Check experience section
        if (resumeText.contains("experience")) {
            String expSection = extractSection(resumeText, "experience");
            int jobCount = expSection.split("\\d{2}/\\d{4} - ").length - 1;

            if (jobCount == 0) {
                jobCount = expSection.split("company|organization").length - 1;
            }

            if (jobCount < 1) {
                suggestions.add("Add more details to your experience section");
            } else {
                strengths.add("Detailed experience section with " + jobCount + " positions");
            }

            // Check for metrics
            if (!expSection.matches(".*\\d+.*")) {
                suggestions.add("Quantify achievements with metrics in experience section");
            }
        }

        // Check projects section
        if (resumeText.contains("projects")) {
            String projectsSection = extractSection(resumeText, "projects");
            if (!projectsSection.contains("http")) {
                suggestions.add("Add links to your projects (GitHub, live demos)");
            }
        }
    }

    private void analyzeKeywords(String resumeText, List<String> suggestions, List<String> strengths) {
        List<String> missingKeywords = new ArrayList<>();
        int matchedKeywords = 0;

        for (String keyword : INDUSTRY_KEYWORDS) {
            if (resumeText.contains(keyword)) {
                matchedKeywords++;
            } else {
                // Only suggest adding if the user has related skills in their profile
                if (shouldSuggestKeyword(keyword, resumeText)) {
                    missingKeywords.add(keyword);
                }
            }
        }

        if (matchedKeywords > 5) {
            strengths.add("Contains " + matchedKeywords + " relevant industry keywords");
        }

        if (!missingKeywords.isEmpty()) {
            suggestions.add("Consider adding these relevant keywords: " +
                    String.join(", ", missingKeywords.subList(0, Math.min(5, missingKeywords.size())) +
                            (missingKeywords.size() > 5 ? "..." : "")));
        }
    }

    private boolean shouldSuggestKeyword(String keyword, String resumeText) {
        String relatedTerm = RELATED_TERMS.get(keyword);
        return relatedTerm != null && resumeText.contains(relatedTerm);
    }

    private int calculateScore(String resumeText, int missingSectionsCount) {
        int score = 100 - (missingSectionsCount * 15); // Deduct 15 points per missing section

        // Bonus for good content
        if (resumeText.contains("skills")) {
            String skillsSection = extractSection(resumeText, "skills");
            int skillCount = skillsSection.split("[,•]").length;
            score += Math.min(10, skillCount / 2); // +1 per 2 skills, max +10
        }

        if (resumeText.contains("experience")) {
            String expSection = extractSection(resumeText, "experience");
            if (expSection.matches(".*\\d+.*")) {
                score += 5; // Bonus for quantified achievements
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    private String extractSection(String content, String sectionTitle) {
        Pattern pattern = Pattern.compile("(?i)" + sectionTitle + "[\\s\\-:]*\\n(.*?)(?=\\n\\s*\\n|$)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}