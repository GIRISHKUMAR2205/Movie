package com.movie.mail_service.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.movie.mail_service.entity.MailDelivery;

public interface MailDeliveryRepository extends JpaRepository<MailDelivery, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO mail_delivery (event_id, status, created_at)
            VALUES (:eventId, 'PROCESSING', CURRENT_TIMESTAMP)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int reserve(@Param("eventId") UUID eventId);

    @Modifying
    @Query("UPDATE MailDelivery delivery SET delivery.status = 'DELIVERED', delivery.deliveredAt = :deliveredAt "
            + "WHERE delivery.eventId = :eventId")
    void markDelivered(@Param("eventId") UUID eventId, @Param("deliveredAt") Instant deliveredAt);
}
