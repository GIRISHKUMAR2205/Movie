package com.movie.payment_service.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refunds")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Refund {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payment_id") private Payment payment;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private RefundStatus status;
    @Column(name = "provider_reference", nullable = false, unique = true, length = 160) private String providerReference;
    @Column(nullable = false, length = 500) private String reason;
    @Column(name = "requested_by", nullable = false, length = 255) private String requestedBy;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
}
