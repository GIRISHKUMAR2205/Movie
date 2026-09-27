package com.movie.booking_service.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.movie.booking_service.client.TheaterInventoryClient;
import com.movie.booking_service.client.TheaterInventoryResponse;
import com.movie.booking_service.client.TheaterInventoryResponse.TheaterSeatResponse;
import com.movie.booking_service.config.BookingProperties;
import com.movie.booking_service.config.InternalApiProperties;
import com.movie.booking_service.dto.BookingPaymentQuoteResponse;
import com.movie.booking_service.dto.BookingResponse;
import com.movie.booking_service.dto.BookingSeatResponse;
import com.movie.booking_service.dto.CreateBookingRequest;
import com.movie.booking_service.dto.ShowSeatMapResponse;
import com.movie.booking_service.dto.ShowSeatResponse;
import com.movie.booking_service.entity.Booking;
import com.movie.booking_service.entity.BookingAudit;
import com.movie.booking_service.entity.BookingSeat;
import com.movie.booking_service.entity.BookingStatus;
import com.movie.booking_service.entity.SeatReservationStatus;
import com.movie.booking_service.error.BookingConflictException;
import com.movie.booking_service.error.BookingNotFoundException;
import com.movie.booking_service.error.InternalAuthenticationException;
import com.movie.booking_service.repository.BookingAuditRepository;
import com.movie.booking_service.repository.BookingRepository;
import com.movie.booking_service.repository.BookingSeatRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class BookingApplicationService {
    private static final List<BookingStatus> EXPIRABLE = List.of(
            BookingStatus.PENDING_PAYMENT, BookingStatus.PAYMENT_PROCESSING);

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository seatRepository;
    private final BookingAuditRepository auditRepository;
    private final TheaterInventoryClient theaterClient;
    private final RedisSeatHoldService redisHolds;
    private final BookingProperties bookingProperties;
    private final InternalApiProperties internalProperties;
    private final Clock clock;
    private final Counter created;
    private final Counter conflicts;
    private final Counter expired;
    private final Counter confirmed;

    public BookingApplicationService(BookingRepository bookingRepository,
            BookingSeatRepository seatRepository,
            BookingAuditRepository auditRepository,
            TheaterInventoryClient theaterClient,
            RedisSeatHoldService redisHolds,
            BookingProperties bookingProperties,
            InternalApiProperties internalProperties,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.auditRepository = auditRepository;
        this.theaterClient = theaterClient;
        this.redisHolds = redisHolds;
        this.bookingProperties = bookingProperties;
        this.internalProperties = internalProperties;
        this.clock = clock;
        this.created = meterRegistry.counter("showhub.booking.created");
        this.conflicts = meterRegistry.counter("showhub.booking.seat.conflicts");
        this.expired = meterRegistry.counter("showhub.booking.expired");
        this.confirmed = meterRegistry.counter("showhub.booking.confirmed");
    }

    @Transactional
    public BookingResponse create(String ownerSubject, String idempotencyKey, CreateBookingRequest request) {
        String key = validateIdempotencyKey(idempotencyKey);
        List<Long> requestedSeatIds = validateSeatIds(request.seatIds());
        String fingerprint = fingerprint(request.showId(), requestedSeatIds);
        var existing = bookingRepository.findByOwnerSubjectAndIdempotencyKey(ownerSubject, key);
        if (existing.isPresent()) {
            if (!existing.get().getRequestFingerprint().equals(fingerprint)) {
                throw new BookingConflictException("The idempotency key was already used for different seats.");
            }
            return toResponse(existing.get());
        }

        TheaterInventoryResponse inventory = theaterClient.inventory(request.showId());
        Map<Long, TheaterSeatResponse> inventorySeats = new HashMap<>();
        for (TheaterSeatResponse seat : inventory.seats()) inventorySeats.put(seat.id(), seat);
        List<TheaterSeatResponse> selected = requestedSeatIds.stream().map(seatId -> {
            TheaterSeatResponse seat = inventorySeats.get(seatId);
            if (seat == null) throw new IllegalArgumentException("Seat " + seatId + " does not belong to this show.");
            return seat;
        }).toList();

        Instant now = clock.instant();
        Instant showCutoff = inventory.startsAt().toInstant().minusSeconds(30);
        Instant expiresAt = min(now.plus(bookingProperties.holdTtl()), showCutoff);
        if (!expiresAt.isAfter(now)) throw new BookingConflictException("This show is too close to its start time.");

        UUID bookingId = UUID.randomUUID();
        if (!redisHolds.acquire(request.showId(), requestedSeatIds, bookingId.toString(), Duration.between(now, expiresAt))) {
            conflicts.increment();
            throw new BookingConflictException("One or more selected seats are currently held by another customer.");
        }
        releaseRedisOnRollback(request.showId(), requestedSeatIds, bookingId.toString());

        try {
            Booking booking = new Booking();
            booking.setId(bookingId);
            booking.setOwnerSubject(ownerSubject);
            booking.setShowId(inventory.showId());
            booking.setContentId(inventory.contentId());
            booking.setContentTitle(inventory.contentTitle());
            booking.setTheaterName(inventory.theaterName());
            booking.setAuditoriumName(inventory.auditoriumName());
            booking.setStartsAt(inventory.startsAt());
            booking.setCurrency(inventory.currency());
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
            booking.setExpiresAt(expiresAt);
            booking.setIdempotencyKey(key);
            booking.setRequestFingerprint(fingerprint);
            BigDecimal total = BigDecimal.ZERO;
            for (TheaterSeatResponse selectedSeat : selected) {
                BookingSeat seat = new BookingSeat();
                seat.setShowId(inventory.showId());
                seat.setSeatId(selectedSeat.id());
                seat.setRowLabel(selectedSeat.rowLabel());
                seat.setSeatNumber(selectedSeat.seatNumber());
                seat.setSeatType(selectedSeat.seatTypeName());
                seat.setPrice(selectedSeat.price());
                seat.setReservationStatus(SeatReservationStatus.HELD);
                booking.addSeat(seat);
                total = total.add(selectedSeat.price());
            }
            booking.setTotalAmount(total.setScale(2));
            bookingRepository.saveAndFlush(booking);
            audit(booking, null, BookingStatus.PENDING_PAYMENT, ownerSubject, "Seats held for checkout.");
            created.increment();
            return toResponse(booking);
        } catch (DataIntegrityViolationException error) {
            redisHolds.release(request.showId(), requestedSeatIds, bookingId.toString());
            conflicts.increment();
            throw new BookingConflictException("One or more selected seats were booked concurrently.");
        } catch (RuntimeException error) {
            redisHolds.release(request.showId(), requestedSeatIds, bookingId.toString());
            throw error;
        }
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> list(String ownerSubject) {
        return bookingRepository.findAllByOwnerSubjectOrderByCreatedAtDesc(ownerSubject)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse get(String ownerSubject, UUID bookingId) {
        return toResponse(ownedBooking(ownerSubject, bookingId));
    }

    @Transactional
    public BookingResponse cancel(String ownerSubject, UUID bookingId) {
        Booking booking = ownedBooking(ownerSubject, bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BookingConflictException("Only a booking awaiting payment can be cancelled.");
        }
        release(booking, BookingStatus.CANCELLED, ownerSubject, "Booking cancelled by its owner.");
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public ShowSeatMapResponse seatMap(Long showId) {
        TheaterInventoryResponse inventory = theaterClient.inventory(showId);
        Map<Long, SeatReservationStatus> durable = new HashMap<>();
        for (BookingSeat seat : seatRepository.findActiveByShowId(showId, clock.instant())) {
            durable.put(seat.getSeatId(), seat.getReservationStatus());
        }
        List<Long> sortedIds = inventory.seats().stream().map(TheaterSeatResponse::id).sorted().toList();
        List<Boolean> redisStatuses = redisHolds.held(showId, sortedIds);
        Set<Long> redisHeld = new HashSet<>();
        for (int index = 0; index < sortedIds.size(); index++) {
            if (redisStatuses.get(index)) redisHeld.add(sortedIds.get(index));
        }
        List<ShowSeatResponse> seats = inventory.seats().stream().map(seat -> {
            SeatReservationStatus status = durable.get(seat.id());
            String availability = status == SeatReservationStatus.CONFIRMED ? "BOOKED"
                    : status == SeatReservationStatus.HELD || redisHeld.contains(seat.id()) ? "HELD" : "AVAILABLE";
            return new ShowSeatResponse(seat.id(), seat.rowLabel(), seat.seatNumber(), seat.seatTypeName(),
                    seat.price(), inventory.currency(), availability);
        }).toList();
        return new ShowSeatMapResponse(showId, seats, bookingProperties.holdTtl().toSeconds());
    }

    @Transactional
    public BookingPaymentQuoteResponse startPayment(UUID bookingId, String ownerSubject, String internalApiKey) {
        authenticateInternal(internalApiKey);
        if (ownerSubject == null || ownerSubject.isBlank()) {
            throw new InternalAuthenticationException();
        }
        Booking booking = bookingRepository.findWithSeatsById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));
        if (!MessageDigest.isEqual(booking.getOwnerSubject().getBytes(StandardCharsets.UTF_8),
                ownerSubject.getBytes(StandardCharsets.UTF_8))) {
            throw new BookingNotFoundException("Booking not found.");
        }
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT
                && booking.getStatus() != BookingStatus.PAYMENT_PROCESSING) {
            throw new BookingConflictException("Booking is " + booking.getStatus() + " and cannot be paid.");
        }
        Instant now = clock.instant();
        if (booking.getExpiresAt() == null || !booking.getExpiresAt().isAfter(now)) {
            release(booking, BookingStatus.EXPIRED, "system", "Seat hold expired before payment began.");
            throw new BookingConflictException("The booking seat hold has expired.");
        }
        Instant expiresAt = min(now.plus(bookingProperties.paymentTtl()), booking.getStartsAt().toInstant().minusSeconds(30));
        if (!expiresAt.isAfter(now)) throw new BookingConflictException("The show is too close to its start time.");
        List<Long> seatIds = seatIds(booking);
        if (!redisHolds.ensure(booking.getShowId(), seatIds, booking.getId().toString(), Duration.between(now, expiresAt))) {
            throw new BookingConflictException("The seat hold could not be extended for payment.");
        }
        releaseRedisOnRollback(booking.getShowId(), seatIds, booking.getId().toString());
        BookingStatus previous = booking.getStatus();
        booking.setStatus(BookingStatus.PAYMENT_PROCESSING);
        booking.setExpiresAt(expiresAt);
        if (previous != BookingStatus.PAYMENT_PROCESSING) {
            audit(booking, previous, BookingStatus.PAYMENT_PROCESSING, "payment-service", "Payment initiated.");
        }
        return new BookingPaymentQuoteResponse(booking.getId(), booking.getOwnerSubject(), booking.getTotalAmount(),
                booking.getCurrency(), booking.getStatus(), booking.getExpiresAt());
    }

    @Transactional
    public int expireDueBookings() {
        List<Booking> due = bookingRepository.findExpired(EXPIRABLE, clock.instant(),
                PageRequest.of(0, bookingProperties.expiryBatchSize()));
        for (Booking booking : due) {
            release(booking, BookingStatus.EXPIRED, "system", "Seat hold expired.");
            expired.increment();
        }
        return due.size();
    }

    void confirm(Booking booking, String actor, String reason) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) return;
        if (booking.getStatus() != BookingStatus.PAYMENT_PROCESSING
                && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BookingConflictException("Cannot confirm a booking in status " + booking.getStatus() + ".");
        }
        BookingStatus previous = booking.getStatus();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null);
        booking.getSeats().forEach(seat -> seat.setReservationStatus(SeatReservationStatus.CONFIRMED));
        safeReleaseRedis(booking);
        audit(booking, previous, BookingStatus.CONFIRMED, actor, reason);
        confirmed.increment();
    }

    void paymentFailed(Booking booking, String actor, String reason) {
        if (booking.getStatus() == BookingStatus.PAYMENT_FAILED || booking.getStatus() == BookingStatus.CANCELLED) return;
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BookingConflictException("A confirmed booking cannot transition to payment failed.");
        }
        release(booking, BookingStatus.PAYMENT_FAILED, actor, reason);
    }

    void refunded(Booking booking, String actor, String reason) {
        if (booking.getStatus() == BookingStatus.REFUNDED) return;
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingConflictException("Only a confirmed booking can be refunded.");
        }
        release(booking, BookingStatus.REFUNDED, actor, reason);
    }

    private void release(Booking booking, BookingStatus target, String actor, String reason) {
        BookingStatus previous = booking.getStatus();
        booking.setStatus(target);
        booking.setExpiresAt(null);
        booking.getSeats().forEach(seat -> seat.setReservationStatus(SeatReservationStatus.RELEASED));
        safeReleaseRedis(booking);
        audit(booking, previous, target, actor, reason);
    }

    private void safeReleaseRedis(Booking booking) {
        try {
            redisHolds.release(booking.getShowId(), seatIds(booking), booking.getId().toString());
        } catch (RuntimeException ignored) {
            // Redis holds expire automatically; PostgreSQL is the durable authority.
        }
    }

    private void releaseRedisOnRollback(Long showId, List<Long> seatIds, String token) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    try {
                        redisHolds.release(showId, seatIds, token);
                    } catch (RuntimeException ignored) {
                        // The lease has a TTL and the database transaction was not committed.
                    }
                }
            }
        });
    }

    private Booking ownedBooking(String ownerSubject, UUID bookingId) {
        return bookingRepository.findByIdAndOwnerSubject(bookingId, ownerSubject)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found."));
    }

    private void audit(Booking booking, BookingStatus previous, BookingStatus current, String actor, String reason) {
        BookingAudit audit = new BookingAudit();
        audit.setBooking(booking);
        audit.setPreviousStatus(previous);
        audit.setCurrentStatus(current);
        audit.setActor(actor);
        audit.setReason(reason);
        auditRepository.save(audit);
    }

    private List<Long> validateSeatIds(List<Long> seatIds) {
        if (seatIds.size() > bookingProperties.maxSeatsPerBooking()) {
            throw new IllegalArgumentException("A booking may contain at most "
                    + bookingProperties.maxSeatsPerBooking() + " seats.");
        }
        List<Long> sorted = seatIds.stream().sorted().toList();
        if (new HashSet<>(sorted).size() != sorted.size()) {
            throw new IllegalArgumentException("A seat may only appear once in a booking.");
        }
        return sorted;
    }

    private String validateIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key is required and must not exceed 128 characters.");
        }
        return value.trim();
    }

    private String fingerprint(Long showId, List<Long> seatIds) {
        try {
            String canonical = showId + ":" + seatIds;
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 must be available.", error);
        }
    }

    private void authenticateInternal(String suppliedKey) {
        if (suppliedKey == null || !MessageDigest.isEqual(
                internalProperties.apiKey().getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new InternalAuthenticationException();
        }
    }

    private Instant min(Instant first, Instant second) {
        return first.isBefore(second) ? first : second;
    }

    private List<Long> seatIds(Booking booking) {
        return booking.getSeats().stream().map(BookingSeat::getSeatId).toList();
    }

    BookingResponse toResponse(Booking booking) {
        List<BookingSeatResponse> seats = new ArrayList<>();
        for (BookingSeat seat : booking.getSeats()) {
            seats.add(new BookingSeatResponse(seat.getSeatId(), seat.getRowLabel(), seat.getSeatNumber(),
                    seat.getSeatType(), seat.getPrice()));
        }
        return new BookingResponse(booking.getId(), booking.getShowId(), booking.getContentTitle(),
                booking.getTheaterName(), booking.getAuditoriumName(), booking.getStartsAt(), seats,
                booking.getTotalAmount(), booking.getCurrency(), booking.getStatus(), booking.getExpiresAt(),
                booking.getCreatedAt());
    }
}
