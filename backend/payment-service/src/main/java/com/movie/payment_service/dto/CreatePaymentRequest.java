package com.movie.payment_service.dto;

import java.util.UUID;

import com.movie.payment_service.entity.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull UUID bookingId,
        @NotNull PaymentMethod paymentMethod) {
}
