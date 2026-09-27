package com.movie.movie_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.movie_service.entity.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByIdAndPublishedTrue(Long id);
    List<Movie> findAllByPublishedTrueOrderByReleaseDateDescTitleAsc();
}
