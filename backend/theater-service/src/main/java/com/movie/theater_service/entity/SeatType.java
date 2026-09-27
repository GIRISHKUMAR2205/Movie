package com.movie.theater_service.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seat_types")
@Getter
@Setter
@NoArgsConstructor
public class SeatType extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditorium_id", nullable = false)
    private Auditorium auditorium;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "default_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal defaultPrice;
}
