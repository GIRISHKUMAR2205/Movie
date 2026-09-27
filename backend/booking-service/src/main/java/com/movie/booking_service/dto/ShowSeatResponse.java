package com.movie.booking_service.dto;

import java.math.BigDecimal;

public record ShowSeatResponse(
        Long id,
        String rowLabel,
        int seatNumber,
        String seatType,
        BigDecimal price,
        String currency,
        String status) {
}
