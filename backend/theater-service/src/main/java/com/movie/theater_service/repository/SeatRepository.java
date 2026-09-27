package com.movie.theater_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.movie.theater_service.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    boolean existsByAuditoriumIdAndRowLabelAndSeatNumber(Long auditoriumId, String rowLabel, int seatNumber);

    @EntityGraph(attributePaths = { "seatType" })
    List<Seat> findAllByAuditoriumIdOrderByRowLabelAscSeatNumberAsc(Long auditoriumId);
}
