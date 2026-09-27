package com.movie.booking_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.movie.booking_service.entity.BookingStatus;

public record BookingPaymentQuoteResponse(
        UUID bookingId,
        String ownerSubject,
        BigDecimal amount,
        String currency,
        BookingStatus status,
        Instant expiresAt) {
}
