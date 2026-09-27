package com.movie.payment_service.error;

public class InvalidWebhookException extends RuntimeException {
    public InvalidWebhookException(String message) { super(message); }
}
