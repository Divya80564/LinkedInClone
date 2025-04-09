package com.divya.linkedinclone.service;

import com.divya.linkedinclone.entity.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(User user, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verify Your Account");
        message.setText("Please click the link below to verify your account:\n\n"
                + "http://localhost:8080/api/users/verify?token=" + token
                + "\n\nThis link will expire in 24 hours.");

        try {
            mailSender.send(message);
            System.out.println("Verification email sent to: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send verification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendPasswordResetEmail(User user, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Password Reset Request");
        message.setText("To reset your password, click the link below:\n\n"
                + "http://localhost:8080/api/users/reset-password?token=" + token
                + "\n\nThis link will expire in 1 hour.\n\n"
                + "If you didn't request this, please ignore this email.");

        try {
            mailSender.send(message);
            System.out.println("Password reset email sent to: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send password reset email");
        }
    }
}