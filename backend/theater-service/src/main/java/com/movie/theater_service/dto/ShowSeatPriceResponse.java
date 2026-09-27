package com.movie.theater_service.dto;

import java.math.BigDecimal;

public record ShowSeatPriceResponse(String seatTypeCode, String seatTypeName, BigDecimal price) {
}
