package com.movie.payment_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.payment_service.entity.Refund;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    List<Refund> findAllByPaymentIdOrderByCreatedAtAsc(UUID paymentId);
}
