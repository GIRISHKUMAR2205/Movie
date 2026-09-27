package com.movie.booking_service.error;

public class BookingConflictException extends RuntimeException {
    public BookingConflictException(String message) { super(message); }
}
