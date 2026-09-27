package com.movie.user_service.exceptions;

public class UnverifiedAccountException extends RuntimeException {
    public UnverifiedAccountException(String message) {
        super(message);
    }
}
