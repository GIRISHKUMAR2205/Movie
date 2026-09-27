package com.movie.booking_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "showhub.security.local")
public record JwtProperties(@NotBlank String issuer, @NotBlank String audience, @NotBlank String jwtSigningKey) {
}
