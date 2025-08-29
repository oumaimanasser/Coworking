package com.esprit.microservice.ms_job_board.Services;

import com.esprit.microservice.ms_job_board.models.PasswordResetToken;
import com.esprit.microservice.ms_job_board.models.User;
import com.esprit.microservice.ms_job_board.Repositories.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetTokenService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    public PasswordResetToken createToken(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusHours(1));
        return tokenRepository.save(token);
    }

    public Optional<PasswordResetToken> findByToken(String token){
        return tokenRepository.findByToken(token);
    }

    public void deleteToken(User user){
        tokenRepository.deleteByUser(user);
    }
}
