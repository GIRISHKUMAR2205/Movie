package com.movie.user_service.entity;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "showhub.outbox.relay")
public record OutboxRelayProperties(
        boolean enabled,
        @NotBlank String exchange,
        @NotBlank String routingKey,
        @NotBlank String queue,
        @NotBlank String deadLetterExchange,
        @NotBlank String deadLetterQueue,
        @NotNull Duration fixedDelay,
        @NotNull Duration claimTimeout,
        @NotNull Duration confirmTimeout,
        @NotNull Duration initialRetryDelay,
        @NotNull Duration maxRetryDelay,
        @Min(1) int batchSize) {
}
