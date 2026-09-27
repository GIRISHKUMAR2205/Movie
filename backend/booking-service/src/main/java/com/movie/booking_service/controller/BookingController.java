package com.movie.booking_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.movie.booking_service.dto.BookingPaymentQuoteResponse;
import com.movie.booking_service.dto.BookingResponse;
import com.movie.booking_service.dto.CreateBookingRequest;
import com.movie.booking_service.dto.ShowSeatMapResponse;
import com.movie.booking_service.service.BookingApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class BookingController {
    private final BookingApplicationService bookingService;

    @GetMapping("/api/v1/shows/{showId}/seats")
    ShowSeatMapResponse seats(@PathVariable Long showId) {
        return bookingService.seatMap(showId);
    }

    @PostMapping("/api/v1/bookings")
    ResponseEntity<BookingResponse> create(Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.create(authentication.getName(), idempotencyKey, request));
    }

    @GetMapping("/api/v1/bookings")
    List<BookingResponse> list(Authentication authentication) {
        return bookingService.list(authentication.getName());
    }

    @GetMapping("/api/v1/bookings/{bookingId}")
    BookingResponse get(Authentication authentication, @PathVariable UUID bookingId) {
        return bookingService.get(authentication.getName(), bookingId);
    }

    @PostMapping("/api/v1/bookings/{bookingId}/cancel")
    BookingResponse cancel(Authentication authentication, @PathVariable UUID bookingId) {
        return bookingService.cancel(authentication.getName(), bookingId);
    }

    @PostMapping("/internal/v1/bookings/{bookingId}/payment-intent")
    BookingPaymentQuoteResponse paymentIntent(@PathVariable UUID bookingId,
            @RequestHeader("X-ShowHub-Owner") String ownerSubject,
            @RequestHeader(value = "X-ShowHub-Internal-Key", required = false) String internalApiKey) {
        return bookingService.startPayment(bookingId, ownerSubject, internalApiKey);
    }
}
