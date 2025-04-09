
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
        VerificationToken verificationToken = new VerificationToken(token, user);
        return tokenRepository.save(verificationToken);
    }

    public VerificationToken getVerificationToken(String token) {
        return tokenRepository.findByToken(token).orElse(null);
    }

    public void deleteToken(VerificationToken token) {
        tokenRepository.delete(token);
    }
}