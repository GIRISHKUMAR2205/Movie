package com.movie.theater_service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PublicShowResponse(
        Long id,
        String contentType,
        String contentId,
        String contentTitle,
        String venueName,
        String auditoriumName,
        String city,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        BigDecimal minPrice,
        String currency,
        Long availableSeats) {
}
