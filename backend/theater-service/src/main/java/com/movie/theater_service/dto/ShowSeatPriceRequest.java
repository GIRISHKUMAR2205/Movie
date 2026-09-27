package com.movie.theater_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShowSeatPriceRequest(
        @NotBlank @Size(max = 40) String seatTypeCode,
        @DecimalMin(value = "0.00", inclusive = false) BigDecimal price) {
}
