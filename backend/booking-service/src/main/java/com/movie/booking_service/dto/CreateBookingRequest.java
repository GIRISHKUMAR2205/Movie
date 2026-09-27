package com.movie.booking_service.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
        @NotNull Long showId,
        @NotEmpty @Size(max = 20) List<@NotNull Long> seatIds) {
}
