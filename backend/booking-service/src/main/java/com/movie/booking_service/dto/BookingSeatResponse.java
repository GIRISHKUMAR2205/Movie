package com.movie.booking_service.dto;

import java.math.BigDecimal;

public record BookingSeatResponse(
        Long seatId,
        String rowLabel,
        int seatNumber,
        String seatType,
        BigDecimal price) {
}
