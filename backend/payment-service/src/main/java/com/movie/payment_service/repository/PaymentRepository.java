package com.movie.payment_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.payment_service.entity.Payment;
import com.movie.payment_service.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdAndOwnerSubject(UUID id, String ownerSubject);
    Optional<Payment> findByOwnerSubjectAndIdempotencyKey(String ownerSubject, String idempotencyKey);
    Optional<Payment> findByProviderReference(String providerReference);
    List<Payment> findAllByOwnerSubjectOrderByCreatedAtDesc(String ownerSubject);
    boolean existsByBookingIdAndStatusIn(UUID bookingId, Collection<PaymentStatus> statuses);
}
