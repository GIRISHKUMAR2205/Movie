package com.movie.user_service.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.user_service.entity.EmailVerificationToken;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHashAndConsumedAtIsNullAndExpiresAtAfter(
            String tokenHash, Instant now);

    void deleteByUserId(Long userId);
}
