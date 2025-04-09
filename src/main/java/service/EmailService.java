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
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}