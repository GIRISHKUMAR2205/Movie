package com.movie.booking_service.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentStatusChangedEvent(
        UUID eventId,
        UUID paymentId,
        UUID bookingId,
        String ownerSubject,
        BigDecimal amount,
        String currency,
        String status,
        Instant occurredAt) {
}
