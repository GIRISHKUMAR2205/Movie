package com.movie.mail_service.event;

import java.util.UUID;

/** Contract emitted by user-service's transactional-outbox relay. */
public record EmailVerificationMailEvent(
        UUID eventId,
        String recipient,
        String subject,
        String body) {
}
