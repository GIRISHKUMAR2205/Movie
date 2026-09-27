package com.movie.booking_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.movie.booking_service.entity.BookingAudit;

public interface BookingAuditRepository extends JpaRepository<BookingAudit, Long> {
}
