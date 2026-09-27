package com.movie.theater_service.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateSeatsRequest(@NotEmpty @Size(max = 1000) List<@Valid SeatRequest> seats) {
}
