package com.movie.booking_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.movie.booking_service.entity.BookingStatus;

public record BookingResponse(
        UUID id,
        Long showId,
        String contentTitle,
        String venueName,
        String auditoriumName,
        OffsetDateTime startsAt,
        List<BookingSeatResponse> seats,
        BigDecimal totalAmount,
        String currency,
        BookingStatus status,
        Instant expiresAt,
        Instant createdAt) {
}
