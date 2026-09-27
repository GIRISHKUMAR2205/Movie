package com.movie.payment_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ConfigurationProperties(prefix = "showhub.payment.sandbox")
@Validated
public record PaymentProviderProperties(
        @NotBlank @Size(min = 32) String webhookSecret,
        @NotBlank String checkoutBaseUrl,
        @NotNull Duration webhookTolerance) {
}
