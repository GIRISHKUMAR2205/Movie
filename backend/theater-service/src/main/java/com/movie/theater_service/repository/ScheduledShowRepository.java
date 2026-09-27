package com.movie.theater_service.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.movie.theater_service.entity.ScheduledShow;

public interface ScheduledShowRepository extends JpaRepository<ScheduledShow, Long> {
    @Query("""
            select case when count(show) > 0 then true else false end
            from ScheduledShow show
            where show.auditorium.id = :auditoriumId
              and show.startsAt < :availableAt
              and show.availableAt > :startsAt
            """)
    boolean hasScheduleConflict(@Param("auditoriumId") Long auditoriumId,
            @Param("startsAt") OffsetDateTime startsAt, @Param("availableAt") OffsetDateTime availableAt);

    List<ScheduledShow> findAllByAuditoriumTheaterIdAndStartsAtBetweenOrderByStartsAtAsc(
            Long theaterId, OffsetDateTime from, OffsetDateTime to);

    List<ScheduledShow> findAllByStartsAtAfterOrderByStartsAtAsc(OffsetDateTime startsAt);
}
