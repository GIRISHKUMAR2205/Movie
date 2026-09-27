package com.movie.user_service.service;

import java.util.UUID;

/** Payload published by the transactional outbox relay through RabbitMQ. */
public record EmailVerificationMailEvent(
        UUID eventId,
        String recipient,
        String subject,
        String body) {
}
