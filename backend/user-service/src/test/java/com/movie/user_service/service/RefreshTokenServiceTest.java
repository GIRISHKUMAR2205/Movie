package com.movie.user_service.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.movie.user_service.entity.RefreshToken;
import com.movie.user_service.entity.RefreshTokenProperties;
import com.movie.user_service.entity.User;
import com.movie.user_service.exceptions.InvalidRefreshTokenException;
import com.movie.user_service.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RefreshTokenProperties properties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void rotationRevokesThePresentedTokenAndCreatesANewOpaqueToken() {
        User user = new User();
        user.setId(10L);
        String rawToken = "old-opaque-token";
        RefreshToken current = new RefreshToken();
        current.setUser(user);
        current.setTokenHash(refreshTokenService.hashToken(rawToken));
        current.setFamilyId(UUID.randomUUID());
        current.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));
        when(properties.ttl()).thenReturn(Duration.ofDays(30));

        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawToken);

        assertNotNull(current.getRevokedAt());
        assertNotEquals(rawToken, rotated.rawToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void replayingARotatedTokenRevokesItsEntireFamily() {
        String rawToken = "replayed-token";
        RefreshToken current = new RefreshToken();
        current.setTokenHash(refreshTokenService.hashToken(rawToken));
        current.setFamilyId(UUID.randomUUID());
        current.setRevokedAt(Instant.now());
        when(refreshTokenRepository.findByTokenHash(current.getTokenHash())).thenReturn(Optional.of(current));

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.rotate(rawToken));
        verify(refreshTokenRepository).revokeActiveByFamilyId(any(UUID.class), any(Instant.class));
    }
}
