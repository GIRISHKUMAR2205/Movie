package com.movie.booking_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "showhub.booking")
public record BookingProperties(
        @NotNull Duration holdTtl,
        @NotNull Duration paymentTtl,
        @Min(1) @Max(20) int maxSeatsPerBooking,
        @Min(1) @Max(500) int expiryBatchSize) {
}
