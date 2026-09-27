package com.movie.booking_service.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {
    private final BookingApplicationService bookingService;

    @Scheduled(fixedDelayString = "${showhub.booking.expiry-scan-delay:PT1S}")
    public void expireSeatHolds() {
        bookingService.expireDueBookings();
    }
}
