package com.movie.booking_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.movie.booking_service.client.TheaterInventoryClient;
import com.movie.booking_service.client.TheaterInventoryResponse;
import com.movie.booking_service.client.TheaterInventoryResponse.TheaterSeatResponse;
import com.movie.booking_service.config.BookingProperties;
import com.movie.booking_service.config.InternalApiProperties;
import com.movie.booking_service.dto.CreateBookingRequest;
import com.movie.booking_service.entity.Booking;
import com.movie.booking_service.error.BookingConflictException;
import com.movie.booking_service.repository.BookingAuditRepository;
import com.movie.booking_service.repository.BookingRepository;
import com.movie.booking_service.repository.BookingSeatRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class BookingApplicationServiceTest {
    @Mock BookingRepository bookingRepository;
    @Mock BookingSeatRepository seatRepository;
    @Mock BookingAuditRepository auditRepository;
    @Mock TheaterInventoryClient theaterClient;
    @Mock RedisSeatHoldService redisHolds;

    private BookingApplicationService service;
    private final Instant now = Instant.parse("2026-09-24T10:00:00Z");

    @BeforeEach
    void setUp() {
        service = new BookingApplicationService(bookingRepository, seatRepository, auditRepository,
                theaterClient, redisHolds,
                new BookingProperties(Duration.ofMinutes(10), Duration.ofMinutes(30), 10, 100),
                new InternalApiProperties("01234567890123456789012345678901", "http://theater", "X-ShowHub-Owner"),
                Clock.fixed(now, ZoneOffset.UTC), new SimpleMeterRegistry());
    }

    @Test
    void pricesBookingFromTheaterInventoryAndPersistsAllSeats() {
        CreateBookingRequest request = new CreateBookingRequest(44L, List.of(3L, 2L));
        when(bookingRepository.findByOwnerSubjectAndIdempotencyKey("user@example.com", "attempt-1"))
                .thenReturn(Optional.empty());
        when(theaterClient.inventory(44L)).thenReturn(inventory());
        when(redisHolds.acquire(any(), any(), any(), any())).thenReturn(true);
        when(bookingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create("user@example.com", "attempt-1", request);

        assertThat(response.totalAmount()).isEqualByComparingTo("700.00");
        assertThat(response.currency()).isEqualTo("INR");
        assertThat(response.seats()).extracting("seatId").containsExactly(2L, 3L);
        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    void rejectsTheWholeRequestWhenRedisCannotAtomicallyHoldEverySeat() {
        CreateBookingRequest request = new CreateBookingRequest(44L, List.of(2L, 3L));
        when(bookingRepository.findByOwnerSubjectAndIdempotencyKey("user@example.com", "attempt-2"))
                .thenReturn(Optional.empty());
        when(theaterClient.inventory(44L)).thenReturn(inventory());
        when(redisHolds.acquire(any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.create("user@example.com", "attempt-2", request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("held by another customer");
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsSameBookingForAnIdenticalIdempotencyRetry() {
        Booking existing = new Booking();
        existing.setId(java.util.UUID.randomUUID());
        existing.setOwnerSubject("user@example.com");
        existing.setShowId(44L);
        existing.setContentTitle("Film");
        existing.setTheaterName("Cinema");
        existing.setAuditoriumName("Screen 1");
        existing.setStartsAt(OffsetDateTime.parse("2026-09-24T14:00:00Z"));
        existing.setTotalAmount(new BigDecimal("700.00"));
        existing.setCurrency("INR");
        existing.setStatus(com.movie.booking_service.entity.BookingStatus.PENDING_PAYMENT);
        existing.setRequestFingerprint(fingerprintForKnownRequest());
        when(bookingRepository.findByOwnerSubjectAndIdempotencyKey("user@example.com", "same-key"))
                .thenReturn(Optional.of(existing));

        var response = service.create("user@example.com", "same-key",
                new CreateBookingRequest(44L, List.of(3L, 2L)));

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(theaterClient, never()).inventory(any());
    }

    private TheaterInventoryResponse inventory() {
        return new TheaterInventoryResponse(44L, "9", "Film", 1L, "Cinema", "Bengaluru",
                7L, "Screen 1", OffsetDateTime.parse("2026-09-24T14:00:00Z"),
                OffsetDateTime.parse("2026-09-24T16:00:00Z"), "INR", List.of(
                        new TheaterSeatResponse(2L, "A", 1, "REGULAR", "Regular", new BigDecimal("300.00")),
                        new TheaterSeatResponse(3L, "A", 2, "PREMIUM", "Premium", new BigDecimal("400.00"))));
    }

    private String fingerprintForKnownRequest() {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest("44:[2, 3]".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }
}
