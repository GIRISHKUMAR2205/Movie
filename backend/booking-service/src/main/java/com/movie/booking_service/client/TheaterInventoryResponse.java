package com.movie.booking_service.client;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TheaterInventoryResponse(
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
        List<TheaterSeatResponse> seats) {
    public record TheaterSeatResponse(
            Long id, String rowLabel, int seatNumber,
            String seatTypeCode, String seatTypeName, BigDecimal price) {
    }
}
