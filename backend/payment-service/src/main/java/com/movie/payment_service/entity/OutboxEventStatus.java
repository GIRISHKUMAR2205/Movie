package com.movie.payment_service.entity;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHING,
    PUBLISHED
}
