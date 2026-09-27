package com.movie.theater_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.theater_service.entity.ShowSeatPrice;

public interface ShowSeatPriceRepository extends JpaRepository<ShowSeatPrice, Long> {
    List<ShowSeatPrice> findAllByScheduledShowIdOrderBySeatTypeCodeAsc(Long scheduledShowId);
}
