package com.movie.movie_service.service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movie.movie_service.dto.MovieResponse;
import com.movie.movie_service.dto.MovieUpsertRequest;
import com.movie.movie_service.entity.Movie;
import com.movie.movie_service.error.ResourceNotFoundException;
import com.movie.movie_service.repository.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieCatalogService {
    private static final Set<String> STANDARD_FORMATS = Set.of(
            "2D", "3D", "IMAX", "IMAX_3D", "4DX", "DOLBY_CINEMA", "SCREENX", "MX4D", "VIP");

    private final MovieRepository movieRepository;

    @Transactional
    public MovieResponse create(MovieUpsertRequest request) {
        Movie movie = new Movie();
        apply(movie, request);
        return toResponse(movieRepository.save(movie));
    }

    @Transactional
    public MovieResponse update(Long movieId, MovieUpsertRequest request) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found."));
        apply(movie, request);
        return toResponse(movie);
    }

    @Transactional(readOnly = true)
    public MovieResponse publishedMovie(Long movieId) {
        return movieRepository.findByIdAndPublishedTrue(movieId).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found."));
    }

    @Transactional(readOnly = true)
    public java.util.List<MovieResponse> publishedMovies() {
        return movieRepository.findAllByPublishedTrueOrderByReleaseDateDescTitleAsc().stream().map(this::toResponse).toList();
    }

    private void apply(Movie movie, MovieUpsertRequest request) {
        Set<String> formats = normalized(request.presentationFormats(), true);
        if (formats.stream().anyMatch(format -> !STANDARD_FORMATS.contains(format) && !format.startsWith("CUSTOM:"))) {
            throw new IllegalArgumentException("Unsupported format. Use a known format or the CUSTOM: prefix.");
        }
        Set<String> subtitleLanguages = normalized(request.subtitleLanguages(), false);
        if (request.hasSubtitles() != !subtitleLanguages.isEmpty()) {
            throw new IllegalArgumentException("Subtitle languages must be supplied exactly when subtitles are available.");
        }
        movie.setTitle(request.title().trim());
        movie.setSynopsis(request.synopsis().trim());
        movie.setOriginalLanguage(request.originalLanguage().trim());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setCertification(request.certification().trim());
        movie.setReleaseDate(request.releaseDate());
        movie.setDirector(blankToNull(request.director()));
        movie.setPresentationFormats(formats);
        movie.setHasSubtitles(request.hasSubtitles());
        movie.setSubtitleLanguages(subtitleLanguages);
        movie.setGenres(normalized(request.genres(), false));
        movie.setCastMembers(trimmed(request.castMembers()));
        movie.setPosterUrl(blankToNull(request.posterUrl()));
        movie.setTrailerUrl(blankToNull(request.trailerUrl()));
        movie.setPublished(request.published());
    }

    private Set<String> normalized(Set<String> values, boolean upperCase) {
        if (values == null) return new LinkedHashSet<>();
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String clean = value.trim();
            normalized.add(upperCase ? clean.toUpperCase(Locale.ROOT) : clean);
        }
        return normalized;
    }

    private Set<String> trimmed(Set<String> values) { return normalized(values, false); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private MovieResponse toResponse(Movie movie) {
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getSynopsis(), movie.getOriginalLanguage(),
                movie.getDurationMinutes(), movie.getCertification(), movie.getReleaseDate(), movie.getDirector(),
                Set.copyOf(movie.getPresentationFormats()), movie.isHasSubtitles(), Set.copyOf(movie.getSubtitleLanguages()),
                Set.copyOf(movie.getGenres()), Set.copyOf(movie.getCastMembers()), movie.getPosterUrl(), movie.getTrailerUrl(),
                movie.isPublished(), movie.getCreatedAt(), movie.getUpdatedAt());
    }
}
