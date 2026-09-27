package com.movie.theater_service.dto;

import java.math.BigDecimal;

public record ShowInventorySeatResponse(
        Long id,
        String rowLabel,
        int seatNumber,
        String seatTypeCode,
        String seatTypeName,
        BigDecimal price) {
}
