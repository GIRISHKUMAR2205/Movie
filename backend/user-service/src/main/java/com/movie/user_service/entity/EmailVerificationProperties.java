package com.movie.user_service.entity;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "showhub.email-verification")
public record EmailVerificationProperties(
        @NotBlank String verificationUrl,
        @NotNull Duration tokenTtl) {
}
