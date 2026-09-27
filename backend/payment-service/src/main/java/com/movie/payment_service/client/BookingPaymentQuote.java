package com.movie.payment_service.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingPaymentQuote(
        UUID bookingId,
        String ownerSubject,
        BigDecimal amount,
        String currency,
        String status,
        Instant expiresAt) {
}
