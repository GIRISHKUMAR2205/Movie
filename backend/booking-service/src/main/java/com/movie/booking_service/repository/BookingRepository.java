package com.movie.booking_service.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.movie.booking_service.entity.Booking;
import com.movie.booking_service.entity.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    @EntityGraph(attributePaths = "seats")
    Optional<Booking> findByIdAndOwnerSubject(UUID id, String ownerSubject);

    @EntityGraph(attributePaths = "seats")
    Optional<Booking> findByOwnerSubjectAndIdempotencyKey(String ownerSubject, String idempotencyKey);

    @EntityGraph(attributePaths = "seats")
    List<Booking> findAllByOwnerSubjectOrderByCreatedAtDesc(String ownerSubject);

    @EntityGraph(attributePaths = "seats")
    @Query("select b from Booking b where b.status in :statuses and b.expiresAt <= :now order by b.expiresAt")
    List<Booking> findExpired(@Param("statuses") List<BookingStatus> statuses, @Param("now") Instant now,
            org.springframework.data.domain.Pageable pageable);

    @EntityGraph(attributePaths = "seats")
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findWithSeatsById(@Param("id") UUID id);
}
