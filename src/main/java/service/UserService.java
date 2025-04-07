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
    private ProfileService profileService; // Inject ProfileService

    // Register a new user
    public User registerUser(UserRegistrationRequest registrationRequest) {
        // Check if the email already exists
        if (userRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
            throw new UserNotFoundException("Email already exists");
        }

        // Create and save the User
        User user = new User();
        user.setName(registrationRequest.getName());
        user.setEmail(registrationRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        user.setRoles(Set.of("USER")); // Assign default role

        User savedUser = userRepository.save(user);

        // Create and save the Profile with default values
        Profile profile = new Profile();
        profile.setBio(""); // Default empty bio
        profile.setProfilePicture(""); // Default empty profile picture
        profile.setSkills(""); // Default empty skills
        profile.setExperience(""); // Default empty experience
        profile.setEducation(""); // Default empty education
        profile.setLocation(""); // Default empty location
        profile.setWebsite(""); // Default empty website
        profile.setUser(savedUser); // Associate the profile with the user
        profileService.createOrUpdateProfile(savedUser.getId(), profile); // Save the profile

        return savedUser;
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