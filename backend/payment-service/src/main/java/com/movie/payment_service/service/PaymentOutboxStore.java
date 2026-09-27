package com.movie.payment_service.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movie.payment_service.config.PaymentOutboxProperties;
import com.movie.payment_service.entity.OutboxEventStatus;
import com.movie.payment_service.entity.PaymentOutboxEvent;
import com.movie.payment_service.repository.PaymentOutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentOutboxStore {
    private final PaymentOutboxEventRepository repository;
    private final PaymentOutboxProperties properties;
    private final Clock clock;

    @Transactional
    public List<PaymentOutboxEvent> claim() {
        Instant now = clock.instant();
        repository.releaseExpiredClaims(now);
        List<PaymentOutboxEvent> events = repository.lockNextPending(now, properties.batchSize());
        for (PaymentOutboxEvent event : events) {
            event.setStatus(OutboxEventStatus.PUBLISHING);
            event.setAttemptCount(event.getAttemptCount() + 1);
            event.setLockedUntil(now.plus(properties.claimTimeout()));
            event.setLastError(null);
        }
        return List.copyOf(events);
    }

    @Transactional
    public void published(UUID eventId) {
        repository.findById(eventId).ifPresent(event -> {
            if (event.getStatus() == OutboxEventStatus.PUBLISHING) {
                event.setStatus(OutboxEventStatus.PUBLISHED);
                event.setPublishedAt(clock.instant());
                event.setLockedUntil(null);
                event.setLastError(null);
            }
        });
    }

    @Transactional
    public void requeue(UUID eventId, Instant nextAttempt, String message) {
        repository.findById(eventId).ifPresent(event -> {
            if (event.getStatus() == OutboxEventStatus.PUBLISHING) {
                event.setStatus(OutboxEventStatus.PENDING);
                event.setNextAttemptAt(nextAttempt);
                event.setLockedUntil(null);
                String safe = message == null ? "Unknown publish failure" : message;
                event.setLastError(safe.length() > 2000 ? safe.substring(0, 2000) : safe);
            }
        });
    }
}
