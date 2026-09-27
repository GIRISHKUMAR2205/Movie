package com.movie.user_service.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.movie.user_service.entity.OutboxEvent;
import com.movie.user_service.entity.OutboxEventStatus;
import com.movie.user_service.entity.User;
import com.movie.user_service.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class EmailVerificationOutboxPublisher {

    private static final String EVENT_TYPE = "EmailVerificationRequested";
    private static final String AGGREGATE_TYPE = "mail";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    /**
     * Persists a single immutable command in the caller's database transaction.
     * The token hash is used only as the idempotency key; the mail body carries
     * the one-time verification link required by the recipient.
     */
    public void publish(User user, String tokenHash, String verificationLink, String subject, String body) {
        UUID eventId = UUID.randomUUID();
        EmailVerificationMailEvent event = new EmailVerificationMailEvent(
                eventId, user.getEmail(), subject, body);

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setId(eventId);
        outboxEvent.setAggregateType(AGGREGATE_TYPE);
        outboxEvent.setAggregateId(user.getId().toString());
        outboxEvent.setType(EVENT_TYPE);
        outboxEvent.setIdempotencyKey("email-verification:" + tokenHash);
        outboxEvent.setCreatedAt(Instant.now());
        outboxEvent.setStatus(OutboxEventStatus.PENDING);
        outboxEvent.setAttemptCount(0);
        outboxEvent.setNextAttemptAt(Instant.now());
        try {
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize email verification event", exception);
        }
        outboxEventRepository.save(outboxEvent);
        meterRegistry.counter("showhub.mail.outbox.events.created", "event_type", EVENT_TYPE).increment();
    }
}
