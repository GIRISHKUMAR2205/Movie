package com.movie.booking_service.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "processed_payment_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedPaymentEvent {
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
