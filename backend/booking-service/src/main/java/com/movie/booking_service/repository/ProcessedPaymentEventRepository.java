package com.movie.booking_service.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.movie.booking_service.entity.ProcessedPaymentEvent;

public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEvent, UUID> {
}
