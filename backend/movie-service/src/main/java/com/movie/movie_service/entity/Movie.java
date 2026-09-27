package com.movie.movie_service.entity;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
public class Movie extends BaseEntity {
    @Column(nullable = false, length = 250)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String synopsis;
    @Column(name = "original_language", nullable = false, length = 80)
    private String originalLanguage;
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;
    @Column(nullable = false, length = 20)
    private String certification;
    @Column(name = "release_date")
    private LocalDate releaseDate;
    @Column(length = 160)
    private String director;
    @Column(name = "poster_url", length = 2048)
    private String posterUrl;
    @Column(name = "trailer_url", length = 2048)
    private String trailerUrl;
    @Column(name = "has_subtitles", nullable = false)
    private boolean hasSubtitles;
    @Column(nullable = false)
    private boolean published;

    @ElementCollection
    @CollectionTable(name = "movie_formats", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "format", nullable = false, length = 60)
    private Set<String> presentationFormats = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "movie_subtitle_languages", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "language", nullable = false, length = 80)
    private Set<String> subtitleLanguages = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "movie_genres", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "genre", nullable = false, length = 80)
    private Set<String> genres = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "movie_cast_members", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "cast_member", nullable = false, length = 160)
    private Set<String> castMembers = new LinkedHashSet<>();
}
