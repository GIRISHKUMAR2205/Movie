package com.movie.payment_service.entity;

public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
