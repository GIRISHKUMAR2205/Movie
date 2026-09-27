package com.movie.payment_service.error;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) { super(message); }
}
