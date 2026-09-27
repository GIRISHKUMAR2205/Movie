package com.movie.payment_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.movie.payment_service.entity.PaymentStatus;

public record PaymentStatusChangedEvent(
        UUID eventId,
        UUID paymentId,
        UUID bookingId,
        String ownerSubject,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        Instant occurredAt) {
}
