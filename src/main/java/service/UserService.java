package com.divya.linkedinclone.service;

import com.divya.linkedinclone.dto.UserRegistrationRequest;
import com.divya.linkedinclone.entity.Profile;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.entity.VerificationToken;
import com.divya.linkedinclone.exception.UserNotFoundException;
import com.divya.linkedinclone.repository.UserRepository;
import org.apache.tika.exception.TikaException;
import com.divya.linkedinclone.dto.ParsedResumeData;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationTokenService verificationTokenService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ResumeParserService resumeParserService;

    @Value("${resume.upload.directory}")
    private String resumeUploadDirectory;

    // Register a new user
    public User registerUser(UserRegistrationRequest registrationRequest) {
        if (userRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
            throw new UserNotFoundException("Email already exists");
        }

        User user = new User();
        user.setName(registrationRequest.getName());
        user.setEmail(registrationRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setRoles(Set.of("USER"));
        user.setEnabled(false); // User is not enabled until email is verified

        User savedUser = userRepository.save(user);

        // Create verification token
        VerificationToken verificationToken = verificationTokenService.createVerificationToken(savedUser);

        // Send verification email
        emailService.sendVerificationEmail(savedUser, verificationToken.getToken());

        // Create profile
        Profile profile = new Profile();
        profile.setBio("");
        profile.setProfilePicture("");
        profile.setSkills("");
        profile.setExperience("");
        profile.setEducation("");
        profile.setLocation("");
        profile.setWebsite("");
        profile.setUser(savedUser);
        profileService.createOrUpdateProfile(savedUser.getId(), profile);

        return savedUser;
    }

    public void verifyUser(String token) {
        VerificationToken verificationToken = verificationTokenService.getVerificationToken(token);
        if (verificationToken == null) {
            throw new RuntimeException("Invalid verification token");
        }

        if (verificationToken.isExpired()) {
            verificationTokenService.deleteToken(verificationToken);
            throw new RuntimeException("Token has expired");
        }

        User user = verificationToken.getUser();
        if (user.isEnabled()) {
            throw new RuntimeException("Account already verified");
        }

        user.setEnabled(true);
        userRepository.save(user);
        verificationTokenService.deleteToken(verificationToken);
    }

    // Find a user by email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Load user details by email (required for authentication)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Ensure the password is BCrypt-encoded
        if (!user.getPassword().startsWith("$2a$")) {
            throw new UsernameNotFoundException("User password is not encoded with BCrypt");
        }

        // Return UserDetails object with the encoded password and authorities
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.emptyList() // Add roles/authorities if applicable
        );
    }

    // Get all users for admin view
    public List<Map<String, Object>> getAllUsersForAdmin() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("userId", user.getId());
                    userMap.put("name", user.getName());
                    userMap.put("email", user.getEmail());
                    userMap.put("role", user.getRoles().stream().findFirst().orElse("USER"));
                    return userMap;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getUserByIdForAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRoles().stream().findFirst().orElse("USER"));
        return userMap;
    }

    public void deleteUserByAdmin(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    // Password reset functionality
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // Create and save password reset token
        VerificationToken passwordResetToken = verificationTokenService.createPasswordResetToken(user);

        // Send email with reset link
        emailService.sendPasswordResetEmail(user, passwordResetToken.getToken());
    }

    public void resetPassword(String token, String newPassword) {
        VerificationToken passwordResetToken = verificationTokenService.getPasswordResetToken(token);
        if (passwordResetToken == null) {
            throw new RuntimeException("Invalid password reset token");
        }

        if (passwordResetToken.isExpired()) {
            verificationTokenService.deleteToken(passwordResetToken);
            throw new RuntimeException("Password reset token has expired");
        }

        User user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete the token after use
        verificationTokenService.deleteToken(passwordResetToken);
    }

    // Resume handling methods
    public User uploadResume(Long userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Validate file type
        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType) &&
                !"application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            throw new RuntimeException("Only PDF and DOCX files are allowed");
        }

        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(resumeUploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Delete old resume if exists
        if (user.getResumePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(user.getResumePath()));
            } catch (IOException e) {
                System.err.println("Failed to delete old resume: " + e.getMessage());
            }
        }

        // Update user with new resume info
        user.setResumePath(filePath.toString());
        user.setResumeName(originalFilename);

        // Parse the resume content
        try {
            ParsedResumeData parsedData = resumeParserService.parseResume(filePath);
            user.setParsedResumeText(formatParsedData(parsedData));
        } catch (Exception e) {
            // If parsing fails, just store the file without parsed content
            System.err.println("Failed to parse resume content: " + e.getMessage());
            user.setParsedResumeText("");
        }
        user.setResumeVersion(UUID.randomUUID().toString());

        return userRepository.save(user);
    }

    public Resource loadResumeAsResource(Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.getResumePath() == null) {
            throw new RuntimeException("Resume not found for user");
        }

        Path filePath = Paths.get(user.getResumePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Could not read file: " + user.getResumePath());
        }
    }

    public void deleteResume(Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.getResumePath() == null) {
            throw new RuntimeException("Resume not found for user");
        }

        // Delete file
        Files.deleteIfExists(Paths.get(user.getResumePath()));

        // Update user
        user.setResumePath(null);
        user.setResumeName(null);
        user.setParsedResumeText(null);
        userRepository.save(user);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    public String parseResumeContent(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.getResumePath() == null) {
            throw new RuntimeException("Resume file not found");
        }

        try {
            Path filePath = Paths.get(user.getResumePath());
            ParsedResumeData parsedData = resumeParserService.parseResume(filePath);
            String formattedData = formatParsedData(parsedData);

            // Store parsed content in database
            user.setParsedResumeText(formattedData);
            userRepository.save(user);

            return formattedData;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resume file", e);
        } catch (TikaException | SAXException e) {
            throw new RuntimeException("Failed to parse resume content", e);
        }
    }

    public ParsedResumeData parseUserResume(Long userId) {
        User user = getUserById(userId);

        if (user.getResumePath() == null) {
            throw new RuntimeException("No resume uploaded for user");
        }

        try {
            Path filePath = Paths.get(user.getResumePath());
            ParsedResumeData parsedData = resumeParserService.parseResume(filePath);

            // Update user with parsed data
            user.setParsedResumeText(formatParsedData(parsedData));
            userRepository.save(user);

            return parsedData;
        } catch (IOException | TikaException | SAXException e) {
            throw new RuntimeException("Failed to parse resume", e);
        }
    }

    private String formatParsedData(ParsedResumeData data) {
        StringBuilder sb = new StringBuilder();
        if (data.getName() != null && !data.getName().isEmpty()) {
            sb.append("Name: ").append(data.getName()).append("\n");
        }
        if (data.getEmail() != null && !data.getEmail().isEmpty()) {
            sb.append("Email: ").append(data.getEmail()).append("\n");
        }
        if (data.getPhone() != null && !data.getPhone().isEmpty()) {
            sb.append("Phone: ").append(data.getPhone()).append("\n");
        }
        if (data.getSkills() != null && !data.getSkills().isEmpty()) {
            sb.append("Skills: ").append(data.getSkills()).append("\n");
        }
        if (data.getEducation() != null && !data.getEducation().isEmpty()) {
            sb.append("Education: ").append(data.getEducation()).append("\n");
        }
        if (data.getExperience() != null && !data.getExperience().isEmpty()) {
            sb.append("Experience: ").append(data.getExperience()).append("\n");
        }
        if (data.getSummary() != null && !data.getSummary().isEmpty()) {
            sb.append("Summary: ").append(data.getSummary());
        }
        return sb.toString();
    }

    // Add to .\service\UserService.java

    // Section keywords with synonyms
    private static final List<String> SKILLS_KEYWORDS = Arrays.asList(
            "skills", "technical skills", "key skills", "technologies",
            "technical expertise", "competencies"
    );

    private static final List<String> EXPERIENCE_KEYWORDS = Arrays.asList(
            "experience", "work history", "employment history",
            "professional experience", "work experience"
    );

    private static final List<String> EDUCATION_KEYWORDS = Arrays.asList(
            "education", "academics", "qualifications",
            "educational background", "degrees"
    );

    private static final List<String> PROJECTS_KEYWORDS = Arrays.asList(
            "projects", "key projects", "selected projects",
            "project experience", "project work"
    );

    // In UserService.java
    // In UserService.java
    public int scoreResume(Long userId) {
        User user = getUserById(userId);

        if (user.getResumePath() == null) {
            throw new RuntimeException("No resume uploaded for user");
        }

        // Force re-parse if empty or null
        if (user.getParsedResumeText() == null || user.getParsedResumeText().isEmpty()) {
            parseResumeContent(userId);
            user = getUserById(userId); // Refresh user object
        }

        String resumeText = user.getParsedResumeText().toLowerCase();
        StringBuilder breakdown = new StringBuilder();
        int score = 0;

        // 1. Check for Skills Section (30 points)
        if (resumeText.contains("skills")) {
            score += 30;
            breakdown.append("Skills section found (+30)\n");

            // Bonus for number of skills listed
            String skillsSection = extractSection(resumeText, "skills");
            int skillCount = skillsSection.split("[,•]").length;
            int skillBonus = Math.min(10, skillCount / 2); // +1 for every 2 skills, max +10
            score += skillBonus;
            breakdown.append(String.format("%d skills listed (+%d)\n", skillCount, skillBonus));
        }

        // 2. Check for Experience Section (30 points)
        if (resumeText.contains("experience")) {
            score += 30;
            breakdown.append("Experience section found (+30)\n");

            // Bonus for job entries
            String expSection = extractSection(resumeText, "experience");
            int jobCount = expSection.split("\\d{2}/\\d{4} - ").length - 1;
            int expBonus = Math.min(15, jobCount * 5); // +5 per job, max +15
            score += expBonus;
            breakdown.append(String.format("%d job positions listed (+%d)\n", jobCount, expBonus));
        }

        // 3. Check for Education Section (20 points)
        if (resumeText.contains("education")) {
            score += 20;
            breakdown.append("Education section found (+20)\n");
        }

        // 4. Check for Projects Section (10 points)
        if (resumeText.contains("projects")) {
            score += 10;
            breakdown.append("Projects section found (+10)\n");
        }

        // 5. Keyword Density Bonus (10 points max)
        List<String> keywords = Arrays.asList(
                "java", "spring", "graphql", "api", "database",
                "optimization", "microservice", "security", "scalability"
        );
        long keywordMatches = keywords.stream()
                .filter(resumeText::contains)
                .count();
        int keywordBonus = Math.min(10, (int)keywordMatches * 2);
        score += keywordBonus;
        breakdown.append(String.format("%d technical keywords matched (+%d)\n", keywordMatches, keywordBonus));

        // Cap at 100
        score = Math.min(score, 100);

        // Store breakdown
        user.setScoreBreakdown(breakdown.toString());
        userRepository.save(user);

        return score;
    }

    public String getScoreBreakdown(Long userId) {
        User user = getUserById(userId);
        return user.getScoreBreakdown();
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private int countKeywords(String text, List<String> keywords) {
        return (int) keywords.stream()
                .filter(text::contains)
                .count();
    }

    // In UserService.java
    private String extractSection(String content, String sectionTitle) {
        // Case-insensitive section matching
        Pattern pattern = Pattern.compile("(?i)" + sectionTitle + "[\\s\\-:]*\\n(.*?)(?=\\n\\s*\\n|$)",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

}