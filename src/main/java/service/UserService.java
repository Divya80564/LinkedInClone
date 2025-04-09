package com.divya.linkedinclone.service;

import com.divya.linkedinclone.dto.UserRegistrationRequest;
import com.divya.linkedinclone.entity.Profile;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.divya.linkedinclone.exception.UserNotFoundException;
import java.util.Set;
import java.util.List;
import com.divya.linkedinclone.entity.VerificationToken;
import com.divya.linkedinclone.service.VerificationTokenService;
import com.divya.linkedinclone.service.EmailService;
import com.divya.linkedinclone.service.ProfileService;

import java.util.Collections;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
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
    private EmailService emailService; // We'll create this next

    @Autowired
    private ProfileService profileService; // Inject ProfileService

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

        // Create profile (existing code)
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

    // Add these methods to UserService.java
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
}