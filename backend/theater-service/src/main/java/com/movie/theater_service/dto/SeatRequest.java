package com.movie.theater_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SeatRequest(
        @NotBlank @Size(max = 10) String rowLabel,
        @Min(1) int seatNumber,
        @NotBlank @Size(max = 40) String seatTypeCode) {
}
