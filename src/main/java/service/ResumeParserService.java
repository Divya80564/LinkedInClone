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

    public ParsedResumeData parseResume(Path filePath) throws IOException, TikaException, SAXException {
        try {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            try (InputStream stream = Files.newInputStream(filePath)) {
                parser.parse(stream, handler, metadata, context);

                String content = handler.toString();
                ParsedResumeData data = new ParsedResumeData();

                // Extract from metadata first
                data.setName(metadata.get("dc:title") != null ? metadata.get("dc:title") :
                        metadata.get("meta:author") != null ? metadata.get("meta:author") :
                                extractNameFromContent(content));

                // Safely extract email and phone
                String email = extractEmail(content);
                String phone = extractPhone(content);

                data.setEmail(email != null ? email : "");
                data.setPhone(phone != null ? phone : "");

                // Extract sections with null checks
                String skills = safeExtractSection(content, "skills|technical skills|key skills");
                String education = safeExtractSection(content, "education|academic background");
                String experience = safeExtractSection(content, "experience|work history");
                String summary = safeExtractSection(content, "summary|profile|about");

                data.setSkills(skills != null ? skills : "");
                data.setEducation(education != null ? education : "");
                data.setExperience(experience != null ? experience : "");
                data.setSummary(summary != null ? summary : "");

                return data;
            }
        } catch (Exception e) {
            // Return empty data if parsing fails
            ParsedResumeData data = new ParsedResumeData();
            data.setName("");
            data.setEmail("");
            data.setPhone("");
            data.setSkills("");
            data.setEducation("");
            data.setExperience("");
            data.setSummary("");
            return data;
        }
    }

    private String extractEmail(String content) {
        Matcher matcher = EMAIL_PATTERN.matcher(content);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractPhone(String content) {
        Matcher matcher = PHONE_PATTERN.matcher(content);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractNameFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        String[] lines = content.split("\n");
        return lines.length > 0 && lines[0] != null ? lines[0].trim() : "";
    }

    private String safeExtractSection(String content, String sectionPattern) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        Pattern pattern = Pattern.compile("(?i)" + sectionPattern + "[\\s\\-:]*\\n(.*?)(?=\\n\\s*\\n|$)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() && matcher.group(1) != null ? matcher.group(1).trim() : "";
    }
}