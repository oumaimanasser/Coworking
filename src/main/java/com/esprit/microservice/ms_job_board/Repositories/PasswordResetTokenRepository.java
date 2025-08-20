package com.esprit.microservice.ms_job_board.Repositories;

import com.esprit.microservice.ms_job_board.models.PasswordResetToken;
import com.esprit.microservice.ms_job_board.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    // Add this method
    void deleteByUser(User user);

    // Or if you prefer to delete by user ID
    void deleteByUserId(Long userId);

    void deleteAllByExpiryDateBefore(LocalDateTime now);
}