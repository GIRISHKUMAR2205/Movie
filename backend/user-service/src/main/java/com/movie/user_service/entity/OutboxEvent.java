package com.movie.user_service.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transactional outbox command. The relay updates delivery metadata, but the
 * command payload itself is immutable.
 */
@Entity
@Table(name = "outbox_event", uniqueConstraints = {
        @UniqueConstraint(name = "uq_outbox_event_idempotency", columnNames = "idempotency_key")
})
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregatetype", nullable = false, length = 255)
    private String aggregateType;

    @Column(name = "aggregateid", nullable = false, length = 255)
    private String aggregateId;

    @Column(nullable = false, length = 255)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error")
    private String lastError;
}
