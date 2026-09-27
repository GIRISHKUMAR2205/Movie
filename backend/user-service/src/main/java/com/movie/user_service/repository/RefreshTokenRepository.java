package com.movie.user_service.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.movie.user_service.entity.RefreshToken;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            UPDATE RefreshToken token
            SET token.revokedAt = :revokedAt
            WHERE token.familyId = :familyId AND token.revokedAt IS NULL
            """)
    int revokeActiveByFamilyId(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("""
            UPDATE RefreshToken token
            SET token.revokedAt = :revokedAt
            WHERE token.user.id = :userId AND token.revokedAt IS NULL
            """)
    int revokeActiveByUserId(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);
}
