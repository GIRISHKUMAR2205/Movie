package com.movie.booking_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "showhub.payment-events")
public record PaymentEventProperties(
        @NotBlank String exchange,
        @NotBlank String routingKey,
        @NotBlank String queue,
        @NotBlank String deadLetterExchange,
        @NotBlank String deadLetterQueue) {
}
