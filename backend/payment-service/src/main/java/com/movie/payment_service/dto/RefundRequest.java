package com.movie.payment_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundRequest(
        @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 500) String reason) {
}
