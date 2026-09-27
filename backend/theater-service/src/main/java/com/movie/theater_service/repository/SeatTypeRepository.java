package com.movie.theater_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.theater_service.entity.SeatType;

public interface SeatTypeRepository extends JpaRepository<SeatType, Long> {
    boolean existsByAuditoriumIdAndCodeIgnoreCase(Long auditoriumId, String code);
    Optional<SeatType> findByAuditoriumIdAndCodeIgnoreCase(Long auditoriumId, String code);
    List<SeatType> findAllByAuditoriumIdOrderByCodeAsc(Long auditoriumId);
}
