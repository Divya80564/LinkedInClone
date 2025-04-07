// .\controller\AdminController.java
package com.divya.linkedinclone.controller;

import com.divya.linkedinclone.dto.AdminLoginRequest;
import com.divya.linkedinclone.dto.AdminRegistrationRequest;
import com.divya.linkedinclone.dto.AdminResponse;
import com.divya.linkedinclone.entity.Admin;
import com.divya.linkedinclone.service.AdminService;
import com.divya.linkedinclone.service.UserService;
import com.divya.linkedinclone.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;


@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;


    @PostMapping("/register")
    public ResponseEntity<?> registerAdmin(@RequestBody AdminRegistrationRequest registrationRequest) {
        try {
            Admin admin = adminService.registerAdmin(registrationRequest);
            AdminResponse response = new AdminResponse(
                    admin.getId(),
                    admin.getUsername(),
                    admin.getEmail(),
                    admin.getRole(),
                    admin.getIsActive(),
                    admin.getCreatedAt(),
                    admin.getUpdatedAt()
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@RequestBody AdminLoginRequest loginRequest) {
        try {
            // Use the admin-specific authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Generate token
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String jwt = jwtUtil.generateToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("role", "ADMIN");
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            // This should be implemented in UserService to return simplified user data
            List<Map<String, Object>> users = userService.getAllUsersForAdmin();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch users"));
        }
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            Map<String, Object> user = userService.getUserByIdForAdmin(userId);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUserByAdmin(userId);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}