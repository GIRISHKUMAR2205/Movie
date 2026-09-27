package com.movie.theater_service.error;

public class InternalAuthenticationException extends RuntimeException {
    public InternalAuthenticationException() {
        super("Internal service authentication failed.");
    }
}
