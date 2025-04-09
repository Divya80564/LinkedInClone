// .\service\VerificationTokenService.java
package com.divya.linkedinclone.service;

import com.divya.linkedinclone.entity.User;
import com.divya.linkedinclone.entity.VerificationToken;
import com.divya.linkedinclone.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class VerificationTokenService {
    @Autowired
    private VerificationTokenRepository tokenRepository;

    public VerificationToken createVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user, VerificationToken.TokenType.VERIFICATION);
        return tokenRepository.save(verificationToken);
    }

    public VerificationToken createPasswordResetToken(User user) {
        // Password reset tokens expire in 1 hour
        String token = UUID.randomUUID().toString();
        VerificationToken passwordResetToken = new VerificationToken(token, user,
                VerificationToken.TokenType.PASSWORD_RESET, 1);
        return tokenRepository.save(passwordResetToken);
    }

    public VerificationToken getVerificationToken(String token) {
        return tokenRepository.findByToken(token).orElse(null);
    }

    public VerificationToken getPasswordResetToken(String token) {
        return tokenRepository.findByTokenAndTokenType(token, VerificationToken.TokenType.PASSWORD_RESET)
                .orElse(null);
    }

    public void deleteToken(VerificationToken token) {
        tokenRepository.delete(token);
    }
}