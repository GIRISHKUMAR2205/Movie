package com.movie.payment_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.movie.payment_service.entity.PaymentMethod;
import com.movie.payment_service.entity.PaymentStatus;

public record PaymentResponse(
        UUID id,
        UUID bookingId,
        BigDecimal amount,
        BigDecimal refundedAmount,
        String currency,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String providerReference,
        String checkoutUrl,
        String failureCode,
        String failureMessage,
        Instant createdAt,
        Instant updatedAt) {
}
