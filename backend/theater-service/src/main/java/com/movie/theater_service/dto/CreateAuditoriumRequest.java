package com.movie.theater_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAuditoriumRequest(@NotBlank @Size(max = 100) String name) {
}
