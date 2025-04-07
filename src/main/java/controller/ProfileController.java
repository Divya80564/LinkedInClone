package com.divya.linkedinclone.controller;

import java.util.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import com.divya.linkedinclone.entity.Profile;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.exception.UserNotFoundException;
import com.divya.linkedinclone.repository.ProfileRepository;
import com.divya.linkedinclone.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.divya.linkedinclone.util.JwtUtil;
import org.springframework.web.bind.annotation.*;
import com.divya.linkedinclone.repository.UserRepository;
@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository; // Autowire ProfileRepository

    // Create or update a profile
    @PostMapping("/{userId}")
    public ResponseEntity<?> createOrUpdateProfile(
            @PathVariable Long userId,
            @RequestBody Profile profile,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Your existing logic from both methods combined
        if (authHeader != null) {
            // Authorization check logic from second method
            String token = authHeader.substring(7);
            String email = jwtUtil.extractUsername(token);
            User requestingUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if (!requestingUser.getId().equals(userId)) {
                boolean isAdmin = requestingUser.getRoles().contains("ADMIN");
                if (!isAdmin) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "You can only edit your own profile."));
                }
            }
        }

        Profile savedProfile = profileService.createOrUpdateProfile(userId, profile);
        return ResponseEntity.ok(savedProfile);
    }
    // Get profile by user ID
    @GetMapping("/{userId}")
    public ResponseEntity<?> getProfileByUserId(@PathVariable Long userId) {
        Profile profile = profileService.getProfileByUserId(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("id", profile.getId());
        response.put("user", Map.of(
                "id", profile.getUser().getId(),
                "name", profile.getUser().getName(),
                "email", profile.getUser().getEmail()
        ));
        response.put("bio", profile.getBio());
        response.put("profilePicture", profile.getProfilePicture());
        response.put("skills", profile.getSkills());
        response.put("experience", profile.getExperience());
        response.put("education", profile.getEducation());
        response.put("location", profile.getLocation());
        response.put("website", profile.getWebsite());
        return ResponseEntity.ok(response);
    }

    // Get all profiles
    @GetMapping
    public ResponseEntity<List<Profile>> getAllProfiles() {
        List<Profile> profiles = profileRepository.findAll();
        return ResponseEntity.ok(profiles);
    }

    // Update a profile by ID
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody Profile updatedProfile) {
        // Check if the profile exists
        Optional<Profile> optionalProfile = profileRepository.findById(id);

        if (optionalProfile.isPresent()) {
            // Update the existing profile
            Profile profile = optionalProfile.get();
            profile.setBio(updatedProfile.getBio());
            profile.setProfilePicture(updatedProfile.getProfilePicture());
            profile.setSkills(updatedProfile.getSkills());
            profile.setExperience(updatedProfile.getExperience());
            profile.setEducation(updatedProfile.getEducation());
            profile.setLocation(updatedProfile.getLocation());
            profile.setWebsite(updatedProfile.getWebsite());

            // Save the updated profile
            Profile savedProfile = profileRepository.save(profile);
            return ResponseEntity.ok(savedProfile);
        } else {
            // Return a 404 Not Found response if the profile doesn't exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Profile not found with id: " + id));
        }
    }

    // Delete a profile by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        profileService.deleteProfile(id);
        return ResponseEntity.ok(Map.of("message", "Profile deleted successfully"));
    }


}