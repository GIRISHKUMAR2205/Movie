package com.movie.booking_service.error;

public class InternalAuthenticationException extends RuntimeException {
    public InternalAuthenticationException() { super("Internal service authentication failed."); }
}
