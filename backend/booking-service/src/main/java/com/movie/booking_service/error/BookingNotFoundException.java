package com.movie.booking_service.error;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(String message) { super(message); }
}
