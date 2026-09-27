package com.movie.payment_service.error;

public class PaymentConflictException extends RuntimeException {
    public PaymentConflictException(String message) { super(message); }
}
