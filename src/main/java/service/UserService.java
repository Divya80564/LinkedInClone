package com.divya.linkedinclone.service;

import com.divya.linkedinclone.dto.UserRegistrationRequest;
import com.divya.linkedinclone.entity.Profile;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.entity.VerificationToken;
import com.divya.linkedinclone.exception.UserNotFoundException;
import com.divya.linkedinclone.repository.UserRepository;
import org.apache.tika.exception.TikaException;
import com.divya.linkedinclone.dto.ParsedResumeData;

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

}