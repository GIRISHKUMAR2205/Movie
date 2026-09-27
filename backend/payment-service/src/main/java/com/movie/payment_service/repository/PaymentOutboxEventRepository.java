package com.movie.payment_service.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.movie.payment_service.entity.PaymentOutboxEvent;

public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEvent, UUID> {
    @Query(value = """
            SELECT * FROM payment_outbox_events
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<PaymentOutboxEvent> lockNextPending(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Modifying
    @Query(value = """
            UPDATE payment_outbox_events
            SET status = 'PENDING', locked_until = NULL, next_attempt_at = :now,
                last_error = 'Publishing lease expired'
            WHERE status = 'PUBLISHING' AND locked_until < :now
            """, nativeQuery = true)
    int releaseExpiredClaims(@Param("now") Instant now);
}
