package com.movie.user_service.exceptions;

public class InvalidVerificationTokenException extends RuntimeException {
    public InvalidVerificationTokenException() {
        super("The verification link is invalid or has expired.");
    }
}
