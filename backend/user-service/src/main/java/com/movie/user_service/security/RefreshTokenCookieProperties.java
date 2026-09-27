package com.movie.user_service.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "showhub.security.refresh-cookie")
public record RefreshTokenCookieProperties(
        @NotBlank String name,
        String domain,
        @NotBlank String path,
        boolean secure,
        @NotBlank String sameSite) {
}
