package com.movie.user_service.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movie.user_service.entity.OutboxEvent;
import com.movie.user_service.entity.OutboxEventStatus;
import com.movie.user_service.entity.OutboxRelayProperties;
import com.movie.user_service.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class OutboxRelayStore {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRelayProperties properties;

    @Transactional
    public List<OutboxEvent> claimNextBatch() {
        Instant now = Instant.now();
        outboxEventRepository.releaseExpiredClaims(now);

        List<OutboxEvent> events = outboxEventRepository.lockNextPending(now, properties.batchSize());
        Instant lockedUntil = now.plus(properties.claimTimeout());
        for (OutboxEvent event : events) {
            event.setStatus(OutboxEventStatus.PUBLISHING);
            event.setAttemptCount(event.getAttemptCount() + 1);
            event.setLockedUntil(lockedUntil);
            event.setLastError(null);
        }
        return List.copyOf(events);
    }

    @Transactional
    public void markPublished(UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            if (event.getStatus() == OutboxEventStatus.PUBLISHING) {
                event.setStatus(OutboxEventStatus.PUBLISHED);
                event.setPublishedAt(Instant.now());
                event.setLockedUntil(null);
                event.setLastError(null);
            }
        });
    }

    @Transactional
    public void requeue(UUID eventId, Instant nextAttemptAt, String error) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            if (event.getStatus() == OutboxEventStatus.PUBLISHING) {
                event.setStatus(OutboxEventStatus.PENDING);
                event.setLockedUntil(null);
                event.setNextAttemptAt(nextAttemptAt);
                event.setLastError(error.length() > 2000 ? error.substring(0, 2000) : error);
            }
        });
    }
}
