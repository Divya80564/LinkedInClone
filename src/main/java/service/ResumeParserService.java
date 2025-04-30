package com.divya.linkedinclone.service;

import com.divya.linkedinclone.dto.ParsedResumeData;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeParserService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("\\b(\\+\\d{1,3}[- ]?)?\\d{10}\\b");

    // In ResumeParserService.java
    public ParsedResumeData parseResume(Path filePath) throws IOException, TikaException, SAXException {
        try {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // -1 for no write limit
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            try (InputStream stream = Files.newInputStream(filePath)) {
                parser.parse(stream, handler, metadata, context);

                // Get the full text content
                String content = handler.toString();

                // Create response object with all fields
                ParsedResumeData data = new ParsedResumeData();
                data.setName(extractName(content, metadata));
                data.setEmail(extractEmail(content));
                data.setPhone(extractPhone(content));
                data.setSkills(extractSection(content, "SKILLS"));
                data.setEducation(extractSection(content, "EDUCATION"));
                data.setExperience(extractSection(content, "EXPERIENCE"));
                data.setSummary(content.substring(0, Math.min(500, content.length()))); // First 500 chars as summary

                return data;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse resume: " + e.getMessage(), e);
        }
    }

    private String extractName(String content, Metadata metadata) {
        // Try metadata first
        String name = metadata.get("dc:title");
        if (name == null) name = metadata.get("meta:author");

        // Fallback to content analysis
        if (name == null || name.isEmpty()) {
            String[] lines = content.split("\n");
            if (lines.length > 0) {
                // First line that looks like a name (contains uppercase words)
                for (String line : lines) {
                    if (line.matches("^[A-Z][a-zA-Z ]+$")) {
                        return line.trim();
                    }
                }
            }
        }
        return name != null ? name : "";
    }

    private String extractEmail(String content) {
        Matcher matcher = EMAIL_PATTERN.matcher(content);
        return matcher.find() ? matcher.group() : "";
    }

    private String extractPhone(String content) {
        Matcher matcher = PHONE_PATTERN.matcher(content);
        return matcher.find() ? matcher.group() : "";
    }

    private String extractSection(String content, String sectionTitle) {
        // Case-insensitive section matching
        Pattern pattern = Pattern.compile("(?i)" + sectionTitle + "[\\s\\-:]*\\n(.*?)(?=\\n\\s*\\n|$)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}