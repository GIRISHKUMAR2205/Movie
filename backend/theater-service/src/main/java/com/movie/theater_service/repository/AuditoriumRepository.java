package com.movie.theater_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.theater_service.entity.Auditorium;

public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {
    Optional<Auditorium> findByIdAndTheaterId(Long id, Long theaterId);
    List<Auditorium> findAllByTheaterIdOrderByNameAsc(Long theaterId);
    boolean existsByTheaterIdAndNameIgnoreCase(Long theaterId, String name);
}
