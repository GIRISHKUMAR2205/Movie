package com.movie.user_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movie.user_service.entity.RefreshToken;
import com.movie.user_service.entity.RefreshTokenProperties;
import com.movie.user_service.entity.User;
import com.movie.user_service.exceptions.InvalidRefreshTokenException;
import com.movie.user_service.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
        private final RefreshTokenRepository refreshTokenRepository;
        private final RefreshTokenProperties properties;
        private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String issue(User user) {
        return persist(user, UUID.randomUUID());
    }

    /**
     * Rotates a token exactly once. Reusing a revoked token is treated as theft:
     * every still-active token in its family is revoked before the request fails.
     */
    @Transactional
    public RotatedRefreshToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken current = refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = Instant.now();
        if (current.getRevokedAt() != null) {
            refreshTokenRepository.revokeActiveByFamilyId(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.setRevokedAt(now);
            throw new InvalidRefreshTokenException();
        }

        current.setRevokedAt(now);
        return new RotatedRefreshToken(current.getUser(), persist(current.getUser(), current.getFamilyId()));
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeActiveByUserId(user.getId(), Instant.now());
    }

    private String persist(User user, UUID familyId) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setFamilyId(familyId);
        refreshToken.setExpiresAt(Instant.now().plus(properties.ttl()));
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record RotatedRefreshToken(User user, String rawToken) {
    }

}
