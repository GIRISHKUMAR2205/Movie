package com.movie.booking_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Validated
@ConfigurationProperties(prefix = "showhub.internal")
public record InternalApiProperties(
        @NotBlank @Size(min = 32) String apiKey,
        @NotBlank String theaterServiceUrl,
        @NotBlank String ownerHeader) {
}
