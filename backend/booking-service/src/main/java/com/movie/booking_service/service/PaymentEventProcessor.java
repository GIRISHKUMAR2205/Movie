package com.movie.booking_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movie.booking_service.entity.Booking;
import com.movie.booking_service.entity.ProcessedPaymentEvent;
import com.movie.booking_service.error.BookingConflictException;
import com.movie.booking_service.error.BookingNotFoundException;
import com.movie.booking_service.event.PaymentStatusChangedEvent;
import com.movie.booking_service.repository.BookingRepository;
import com.movie.booking_service.repository.ProcessedPaymentEventRepository;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentEventProcessor {
    private final BookingRepository bookingRepository;
    private final ProcessedPaymentEventRepository processedRepository;
    private final BookingApplicationService bookingService;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void process(PaymentStatusChangedEvent event, String rawPayload) {
        if (event.eventId() == null || event.bookingId() == null || event.paymentId() == null
                || event.status() == null || event.ownerSubject() == null) {
            throw new IllegalArgumentException("Payment event is missing required fields.");
        }
        String payloadHash = hash(rawPayload);
        ProcessedPaymentEvent existing = processedRepository.findById(event.eventId()).orElse(null);
        if (existing != null) {
            if (!existing.getPayloadHash().equals(payloadHash)) {
                throw new BookingConflictException("Payment event ID was reused with different content.");
            }
            meterRegistry.counter("showhub.booking.payment.events.duplicate").increment();
            return;
        }

        Booking booking = bookingRepository.findWithSeatsById(event.bookingId())
                .orElseThrow(() -> new BookingNotFoundException("Booking referenced by payment event was not found."));
        if (!MessageDigest.isEqual(booking.getOwnerSubject().getBytes(StandardCharsets.UTF_8),
                event.ownerSubject().getBytes(StandardCharsets.UTF_8))) {
            throw new BookingConflictException("Payment event owner does not match the booking owner.");
        }
        if (event.amount() != null && booking.getTotalAmount().compareTo(event.amount()) != 0) {
            throw new BookingConflictException("Payment event amount does not match the booking total.");
        }
        if (event.currency() != null && !booking.getCurrency().equalsIgnoreCase(event.currency())) {
            throw new BookingConflictException("Payment event currency does not match the booking currency.");
        }

        switch (event.status()) {
            case "SUCCEEDED" -> bookingService.confirm(booking, "payment:" + event.paymentId(), "Payment succeeded.");
            case "FAILED", "CANCELLED" -> bookingService.paymentFailed(
                    booking, "payment:" + event.paymentId(), "Payment " + event.status().toLowerCase() + ".");
            case "REFUNDED" -> bookingService.refunded(booking, "payment:" + event.paymentId(), "Payment fully refunded.");
            case "PENDING", "PARTIALLY_REFUNDED" -> { /* No booking-state transition. */ }
            default -> throw new IllegalArgumentException("Unsupported payment status " + event.status() + ".");
        }

        ProcessedPaymentEvent processed = new ProcessedPaymentEvent();
        processed.setEventId(event.eventId());
        processed.setPayloadHash(payloadHash);
        processed.setProcessedAt(clock.instant());
        processedRepository.save(processed);
        meterRegistry.counter("showhub.booking.payment.events.processed", "status", event.status()).increment();
    }

    private String hash(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 must be available.", error);
        }
    }
}
