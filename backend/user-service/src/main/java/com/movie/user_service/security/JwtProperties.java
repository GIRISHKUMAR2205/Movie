package com.movie.user_service.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "showhub.security.local")
public record JwtProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotBlank String jwtSigningKey,
        @NotNull Duration accessTokenTtl) {
}
