package com.movie.theater_service.dto;

import java.math.BigDecimal;

public record SeatTypeResponse(Long id, String code, String displayName, BigDecimal defaultPrice) {
}
