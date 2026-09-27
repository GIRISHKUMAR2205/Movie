package com.movie.movie_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.movie.movie_service.dto.MovieResponse;
import com.movie.movie_service.dto.MovieUpsertRequest;
import com.movie.movie_service.service.MovieCatalogService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MovieController {
    private final MovieCatalogService movieCatalogService;

    @GetMapping
    List<MovieResponse> publishedMovies() {
        return movieCatalogService.publishedMovies();
    }

    @GetMapping("/{movieId}")
    MovieResponse publishedMovie(@PathVariable Long movieId) {
        return movieCatalogService.publishedMovie(movieId);
    }

    @PostMapping
    ResponseEntity<MovieResponse> create(@Valid @RequestBody MovieUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movieCatalogService.create(request));
    }

    @PutMapping("/{movieId}")
    MovieResponse update(@PathVariable Long movieId, @Valid @RequestBody MovieUpsertRequest request) {
        return movieCatalogService.update(movieId, request);
    }
}
