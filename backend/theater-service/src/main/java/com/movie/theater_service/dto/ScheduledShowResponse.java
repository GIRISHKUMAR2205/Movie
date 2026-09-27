package com.movie.theater_service.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ScheduledShowResponse(
        Long id,
        Long auditoriumId,
        String auditoriumName,
        String contentType,
        String contentId,
        String contentTitle,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime intermissionStartsAt,
        OffsetDateTime intermissionEndsAt,
        OffsetDateTime availableAt,
        List<ShowSeatPriceResponse> seatPrices) {
}
