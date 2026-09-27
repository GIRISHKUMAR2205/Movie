package com.movie.theater_service.entity;

import java.time.OffsetDateTime;

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
@Table(name = "scheduled_shows")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledShow extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auditorium_id", nullable = false)
    private Auditorium auditorium;

    @Column(name = "content_id", nullable = false, length = 100)
    private String contentId;

    @Column(name = "content_title", nullable = false, length = 250)
    private String contentTitle;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "intermission_starts_at")
    private OffsetDateTime intermissionStartsAt;

    @Column(name = "intermission_ends_at")
    private OffsetDateTime intermissionEndsAt;

    /** The auditorium cannot be used until this post-show turnaround break ends. */
    @Column(name = "available_at", nullable = false)
    private OffsetDateTime availableAt;
}
