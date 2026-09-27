package com.movie.theater_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTheaterRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 100) String country,
        @NotBlank @Size(max = 64) String timeZone) {
}
