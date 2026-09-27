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
@Table(name = "show_seat_prices")
@Getter
@Setter
@NoArgsConstructor
public class ShowSeatPrice extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private ScheduledShow scheduledShow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_type_id", nullable = false)
    private SeatType seatType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
}
