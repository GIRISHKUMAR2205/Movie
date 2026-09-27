package com.movie.movie_service.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record MovieResponse(
        Long id,
        String title,
        String synopsis,
        String originalLanguage,
        int durationMinutes,
        String certification,
        LocalDate releaseDate,
        String director,
        Set<String> presentationFormats,
        boolean hasSubtitles,
        Set<String> subtitleLanguages,
        Set<String> genres,
        Set<String> castMembers,
        String posterUrl,
        String trailerUrl,
        boolean published,
        Instant createdAt,
        Instant updatedAt) {
}
