package com.movie.theater_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSeatTypeRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 100) String displayName,
        @DecimalMin(value = "0.00", inclusive = false) BigDecimal defaultPrice) {
}
