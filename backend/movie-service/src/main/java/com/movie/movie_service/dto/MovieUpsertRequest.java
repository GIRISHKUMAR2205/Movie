package com.movie.movie_service.dto;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record MovieUpsertRequest(
        @NotBlank @Size(max = 250) String title,
        @NotBlank @Size(max = 10000) String synopsis,
        @NotBlank @Size(max = 80) String originalLanguage,
        @Min(1) @Max(600) int durationMinutes,
        @NotBlank @Size(max = 20) String certification,
        LocalDate releaseDate,
        @Size(max = 160) String director,
        @NotEmpty Set<@NotBlank @Size(max = 60) String> presentationFormats,
        boolean hasSubtitles,
        Set<@NotBlank @Size(max = 80) String> subtitleLanguages,
        Set<@NotBlank @Size(max = 80) String> genres,
        Set<@NotBlank @Size(max = 160) String> castMembers,
        @Size(max = 2048) String posterUrl,
        @Size(max = 2048) String trailerUrl,
        boolean published) {
}
