package com.movie.payment_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "showhub.payment-events")
public record PaymentOutboxProperties(
        boolean enabled,
        @NotBlank String exchange,
        @NotBlank String routingKey,
        @NotBlank String queue,
        @NotBlank String deadLetterExchange,
        @NotBlank String deadLetterQueue,
        @NotNull Duration claimTimeout,
        @NotNull Duration confirmTimeout,
        @NotNull Duration initialRetryDelay,
        @NotNull Duration maxRetryDelay,
        @Min(1) int batchSize) {
}
