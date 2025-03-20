package com.divya.linkedinclone.service;


import com.divya.linkedinclone.entity.Profile;
import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.exception.UserNotFoundException;
import com.divya.linkedinclone.exception.ProfileNotFoundException;

import com.divya.linkedinclone.repository.ProfileRepository;
import com.divya.linkedinclone.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    // Create or update a profile for a user
    public Profile createOrUpdateProfile(Long userId, @Valid Profile profile) {
        // Check if a profile already exists for the user
        Profile existingProfile = profileRepository.findByUserId(userId).orElse(null);

        if (existingProfile != null) {
            // Update the existing profile
            existingProfile.setBio(profile.getBio());
            existingProfile.setProfilePicture(profile.getProfilePicture());
            existingProfile.setSkills(profile.getSkills());
            existingProfile.setExperience(profile.getExperience());
            existingProfile.setEducation(profile.getEducation());
            existingProfile.setLocation(profile.getLocation());
            existingProfile.setWebsite(profile.getWebsite());
            return profileRepository.save(existingProfile);
        } else {
            // Create a new profile
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
            profile.setUser(user);
            return profileRepository.save(profile);
        }
    }

    // Get profile by user ID
    public Profile getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user id: " + userId));
    }

    // Delete profile by ID
    public void deleteProfile(Long id) {
        if (!profileRepository.existsById(id)) {
            throw new ProfileNotFoundException("Profile not found with id: " + id);
        }
        profileRepository.deleteById(id);
    }
}