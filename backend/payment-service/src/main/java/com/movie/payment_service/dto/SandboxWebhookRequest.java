package com.movie.payment_service.dto;

import com.movie.payment_service.entity.PaymentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SandboxWebhookRequest(
        @NotBlank @Size(max = 160) String eventId,
        @NotBlank @Size(max = 160) String providerReference,
        @NotNull PaymentStatus status,
        @Size(max = 80) String failureCode,
        @Size(max = 500) String failureMessage) {
}
