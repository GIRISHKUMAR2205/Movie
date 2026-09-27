package com.movie.payment_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.movie.payment_service.entity.RefundStatus;

public record RefundResponse(
        UUID id, UUID paymentId, BigDecimal amount, RefundStatus status,
        String providerReference, String reason, Instant createdAt) {
}
