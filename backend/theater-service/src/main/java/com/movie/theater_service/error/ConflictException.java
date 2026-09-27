package com.movie.theater_service.error;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}
