package com.movie.booking_service.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.movie.booking_service.entity.BookingSeat;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {
    @Query("""
            select seat from BookingSeat seat join fetch seat.booking booking
            where seat.showId = :showId
              and seat.reservationStatus in (com.movie.booking_service.entity.SeatReservationStatus.HELD,
                                             com.movie.booking_service.entity.SeatReservationStatus.CONFIRMED)
              and (seat.reservationStatus = com.movie.booking_service.entity.SeatReservationStatus.CONFIRMED
                   or booking.expiresAt > :now)
            """)
    List<BookingSeat> findActiveByShowId(@Param("showId") Long showId, @Param("now") Instant now);
}
