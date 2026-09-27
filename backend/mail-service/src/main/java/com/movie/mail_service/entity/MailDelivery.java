package com.movie.mail_service.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Inbox row keyed by the producer event ID. */
@Entity
@Table(name = "mail_delivery")
@Getter
@Setter
@NoArgsConstructor
public class MailDelivery {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;
}
