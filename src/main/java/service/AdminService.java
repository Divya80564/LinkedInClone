// .\service\AdminService.java
package com.divya.linkedinclone.service;

import com.divya.linkedinclone.dto.AdminRegistrationRequest;
import com.divya.linkedinclone.entity.Admin;
import com.divya.linkedinclone.exception.AdminNotFoundException;
import com.divya.linkedinclone.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Admin registerAdmin(AdminRegistrationRequest registrationRequest) {
        // Check if username or email already exists
        if (adminRepository.findByUsername(registrationRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (adminRepository.findByEmail(registrationRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Admin admin = new Admin();
        admin.setUsername(registrationRequest.getUsername());
        admin.setEmail(registrationRequest.getEmail());
        admin.setPassword(passwordEncoder.encode(registrationRequest.getPassword()));
        admin.setRole("ADMIN");
        admin.setIsActive(true);

        return adminRepository.save(admin);
    }

    public Admin authenticateAdmin(String username, String password) {
        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found with username: " + username));

        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }

        if (!admin.getIsActive()) {
            throw new RuntimeException("Admin account is inactive");
        }

        return admin;
    }

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    public Admin getAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found with id: " + id));
    }

    public void deleteAdmin(Long id) {
        if (!adminRepository.existsById(id)) {
            throw new AdminNotFoundException("Admin not found with id: " + id);
        }
        adminRepository.deleteById(id);
    }
}