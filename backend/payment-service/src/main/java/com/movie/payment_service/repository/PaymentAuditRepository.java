package com.movie.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.payment_service.entity.PaymentAudit;

public interface PaymentAuditRepository extends JpaRepository<PaymentAudit, Long> {
}
