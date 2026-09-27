package com.movie.payment_service.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_audits")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PaymentAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payment_id") private Payment payment;
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 30) private PaymentStatus previousStatus;
    @Enumerated(EnumType.STRING) @Column(name = "current_status", nullable = false, length = 30) private PaymentStatus currentStatus;
    @Column(nullable = false, length = 255) private String actor;
    @Column(nullable = false, length = 500) private String reason;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
}
