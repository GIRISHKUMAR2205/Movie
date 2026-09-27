package com.movie.booking_service.entity;

public enum BookingStatus {
    PENDING_PAYMENT,
    PAYMENT_PROCESSING,
    CONFIRMED,
    CANCELLED,
    EXPIRED,
    PAYMENT_FAILED,
    REFUNDED
}
