package com.movie.user_service.entity;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "showhub.security.refresh-token")
public record RefreshTokenProperties(@NotNull Duration ttl) {
}
