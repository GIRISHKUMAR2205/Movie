package com.movie.theater_service.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ShowInventoryResponse(
        Long showId,
        String contentId,
        String contentTitle,
        Long theaterId,
        String theaterName,
        String city,
        Long auditoriumId,
        String auditoriumName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String currency,
        List<ShowInventorySeatResponse> seats) {
}
