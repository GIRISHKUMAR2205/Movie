package com.movie.payment_service.error;

public class PaymentDependencyException extends RuntimeException {
    public PaymentDependencyException(String message) { super(message); }
    public PaymentDependencyException(String message, Throwable cause) { super(message, cause); }
}
