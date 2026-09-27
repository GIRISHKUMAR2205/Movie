package com.movie.theater_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movie.theater_service.dto.CreateShowScheduleRequest;
import com.movie.theater_service.dto.ScheduledShowResponse;
import com.movie.theater_service.dto.ShowSeatPriceRequest;
import com.movie.theater_service.dto.ShowSeatPriceResponse;
import com.movie.theater_service.dto.ShowInventoryResponse;
import com.movie.theater_service.dto.ShowInventorySeatResponse;
import com.movie.theater_service.dto.PublicShowResponse;
import com.movie.theater_service.config.InternalApiProperties;
import com.movie.theater_service.entity.Auditorium;
import com.movie.theater_service.entity.ScheduledShow;
import com.movie.theater_service.entity.SeatType;
import com.movie.theater_service.entity.ShowSeatPrice;
import com.movie.theater_service.entity.Theater;
import com.movie.theater_service.error.ConflictException;
import com.movie.theater_service.repository.ScheduledShowRepository;
import com.movie.theater_service.repository.SeatTypeRepository;
import com.movie.theater_service.repository.ShowSeatPriceRepository;
import com.movie.theater_service.repository.TheaterRepository;
import com.movie.theater_service.repository.SeatRepository;
import com.movie.theater_service.entity.Seat;
import com.movie.theater_service.error.InternalAuthenticationException;
import com.movie.theater_service.error.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShowScheduleService {

    private static final int MAX_SCHEDULE_DAYS = 366;

    private final TheaterAdministrationService theaterAdministrationService;
    private final ScheduledShowRepository scheduledShowRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final ShowSeatPriceRepository showSeatPriceRepository;
    private final TheaterRepository theaterRepository;
    private final SeatRepository seatRepository;
    private final InternalApiProperties internalApiProperties;
    private final Clock clock;

    @Transactional
    public List<ScheduledShowResponse> schedule(String ownerSubject, Long theaterId, Long auditoriumId,
            CreateShowScheduleRequest request) {
        Auditorium auditorium = theaterAdministrationService.ownedAuditorium(ownerSubject, theaterId, auditoriumId);
        validateSchedule(request);
        ZoneId zone = ZoneId.of(auditorium.getTheater().getTimeZone());
        Map<SeatType, BigDecimal> prices = resolvedPrices(auditoriumId, request.seatPrices());

        List<ScheduledShow> scheduledShows = request.fromDate().datesUntil(request.toDate().plusDays(1))
                .map(date -> scheduledShow(auditorium, date, request, zone))
                .toList();
        for (ScheduledShow scheduledShow : scheduledShows) {
            if (scheduledShowRepository.hasScheduleConflict(auditoriumId,
                    scheduledShow.getStartsAt(), scheduledShow.getAvailableAt())) {
                throw new ConflictException("The auditorium is unavailable for " + scheduledShow.getStartsAt() + ".");
            }
        }

        List<ScheduledShow> persistedShows = scheduledShowRepository.saveAll(scheduledShows);
        for (ScheduledShow scheduledShow : persistedShows) {
            List<ShowSeatPrice> showPrices = prices.entrySet().stream().map(entry -> {
                ShowSeatPrice price = new ShowSeatPrice();
                price.setScheduledShow(scheduledShow);
                price.setSeatType(entry.getKey());
                price.setPrice(entry.getValue());
                return price;
            }).toList();
            showSeatPriceRepository.saveAll(showPrices);
        }
        return persistedShows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduledShowResponse> scheduleForTheater(Long theaterId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must not be before from.");
        }
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found."));
        ZoneId timeZone = ZoneId.of(theater.getTimeZone());
        OffsetDateTime startsAt = from.atStartOfDay(timeZone).toOffsetDateTime();
        OffsetDateTime endsAt = to.plusDays(1).atStartOfDay(timeZone).toOffsetDateTime();
        return scheduledShowRepository.findAllByAuditoriumTheaterIdAndStartsAtBetweenOrderByStartsAtAsc(
                theaterId, startsAt, endsAt).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ShowInventoryResponse bookingInventory(Long showId, String internalApiKey) {
        authenticateInternal(internalApiKey);
        ScheduledShow show = scheduledShowRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found."));
        if (!show.getStartsAt().toInstant().isAfter(clock.instant())) {
            throw new ConflictException("This show has already started.");
        }
        List<ShowSeatPrice> prices = showSeatPriceRepository
                .findAllByScheduledShowIdOrderBySeatTypeCodeAsc(showId);
        Map<Long, BigDecimal> priceBySeatType = prices.stream().collect(
                java.util.stream.Collectors.toMap(price -> price.getSeatType().getId(), ShowSeatPrice::getPrice));
        List<ShowInventorySeatResponse> seats = seatRepository
                .findAllByAuditoriumIdOrderByRowLabelAscSeatNumberAsc(show.getAuditorium().getId())
                .stream().map(seat -> inventorySeat(seat, priceBySeatType)).toList();
        Theater theater = show.getAuditorium().getTheater();
        return new ShowInventoryResponse(show.getId(), show.getContentId(), show.getContentTitle(),
                theater.getId(), theater.getName(), theater.getCity(), show.getAuditorium().getId(),
                show.getAuditorium().getName(), show.getStartsAt(), show.getEndsAt(),
                internalApiProperties.currency().toUpperCase(Locale.ROOT), seats);
    }

    @Transactional(readOnly = true)
    public List<PublicShowResponse> publicShows(String contentId, String contentType, String city, LocalDate date) {
        return scheduledShowRepository.findAllByStartsAtAfterOrderByStartsAtAsc(
                        OffsetDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC))
                .stream()
                .filter(show -> contentId == null || show.getContentId().equals(contentId))
                .filter(show -> contentType == null || show.getContentType().equalsIgnoreCase(contentType))
                .filter(show -> city == null || show.getAuditorium().getTheater().getCity().equalsIgnoreCase(city))
                .filter(show -> date == null || show.getStartsAt().toLocalDate().equals(date))
                .map(this::toPublicResponse)
                .toList();
    }

    private ShowInventorySeatResponse inventorySeat(Seat seat, Map<Long, BigDecimal> priceBySeatType) {
        BigDecimal price = priceBySeatType.get(seat.getSeatType().getId());
        if (price == null) {
            throw new IllegalStateException("The show has no price for seat type " + seat.getSeatType().getCode() + ".");
        }
        return new ShowInventorySeatResponse(seat.getId(), seat.getRowLabel(), seat.getSeatNumber(),
                seat.getSeatType().getCode(), seat.getSeatType().getDisplayName(), price);
    }

    private void authenticateInternal(String suppliedKey) {
        if (suppliedKey == null || !MessageDigest.isEqual(
                internalApiProperties.apiKey().getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new InternalAuthenticationException();
        }
    }

    private ScheduledShow scheduledShow(Auditorium auditorium, LocalDate date,
            CreateShowScheduleRequest request, ZoneId zone) {
        OffsetDateTime startsAt = date.atTime(request.showTime()).atZone(zone).toOffsetDateTime();
        OffsetDateTime endsAt = startsAt.plusMinutes(request.durationMinutes());
        ScheduledShow scheduledShow = new ScheduledShow();
        scheduledShow.setAuditorium(auditorium);
        scheduledShow.setContentId(request.contentId().trim());
        scheduledShow.setContentTitle(request.contentTitle().trim());
        scheduledShow.setContentType(request.contentType().name());
        scheduledShow.setStartsAt(startsAt);
        scheduledShow.setEndsAt(endsAt);
        if (request.intermissionDurationMinutes() > 0) {
            OffsetDateTime intermissionStartsAt = startsAt.plusMinutes(request.intermissionStartMinute());
            scheduledShow.setIntermissionStartsAt(intermissionStartsAt);
            scheduledShow.setIntermissionEndsAt(intermissionStartsAt.plusMinutes(request.intermissionDurationMinutes()));
        }
        scheduledShow.setAvailableAt(endsAt.plusMinutes(request.postShowBreakMinutes()));
        return scheduledShow;
    }

    private Map<SeatType, BigDecimal> resolvedPrices(Long auditoriumId, List<ShowSeatPriceRequest> requestedPrices) {
        List<SeatType> seatTypes = seatTypeRepository.findAllByAuditoriumIdOrderByCodeAsc(auditoriumId);
        if (seatTypes.isEmpty()) {
            throw new IllegalArgumentException("Configure at least one seat type before scheduling shows.");
        }
        Map<String, BigDecimal> overrides = new HashMap<>();
        if (requestedPrices != null) {
            for (ShowSeatPriceRequest price : requestedPrices) {
                String code = price.seatTypeCode().trim().toUpperCase(Locale.ROOT);
                if (overrides.put(code, price.price()) != null) {
                    throw new IllegalArgumentException("A price was configured more than once for " + code + ".");
                }
            }
        }
        Map<SeatType, BigDecimal> prices = new HashMap<>();
        for (SeatType seatType : seatTypes) {
            BigDecimal price = overrides.remove(seatType.getCode());
            prices.put(seatType, normalizeMoney(price == null ? seatType.getDefaultPrice() : price));
        }
        if (!overrides.isEmpty()) {
            throw new IllegalArgumentException("Unknown seat type in price configuration: " + overrides.keySet().iterator().next());
        }
        return prices;
    }

    private void validateSchedule(CreateShowScheduleRequest request) {
        if (request.toDate().isBefore(request.fromDate())) {
            throw new IllegalArgumentException("toDate must not be before fromDate.");
        }
        if (ChronoUnit.DAYS.between(request.fromDate(), request.toDate()) > MAX_SCHEDULE_DAYS) {
            throw new IllegalArgumentException("A schedule can cover at most " + MAX_SCHEDULE_DAYS + " days.");
        }
        boolean hasIntermission = request.intermissionDurationMinutes() > 0;
        if (hasIntermission && request.intermissionStartMinute() == null) {
            throw new IllegalArgumentException("intermissionStartMinute is required when configuring an intermission.");
        }
        if (!hasIntermission && request.intermissionStartMinute() != null) {
            throw new IllegalArgumentException("intermissionDurationMinutes must be positive when an intermission start is set.");
        }
        if (hasIntermission && (request.intermissionStartMinute() <= 0
                || request.intermissionStartMinute() + request.intermissionDurationMinutes() >= request.durationMinutes())) {
            throw new IllegalArgumentException("The intermission must be entirely within the show duration.");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Prices must have at most two decimal places.");
        }
        return amount.setScale(2);
    }

    private ScheduledShowResponse toResponse(ScheduledShow show) {
        List<ShowSeatPriceResponse> prices = showSeatPriceRepository.findAllByScheduledShowIdOrderBySeatTypeCodeAsc(show.getId())
                .stream().map(price -> new ShowSeatPriceResponse(
                        price.getSeatType().getCode(), price.getSeatType().getDisplayName(), price.getPrice())).toList();
        return new ScheduledShowResponse(show.getId(), show.getAuditorium().getId(), show.getAuditorium().getName(),
                show.getContentType(), show.getContentId(), show.getContentTitle(), show.getStartsAt(), show.getEndsAt(),
                show.getIntermissionStartsAt(), show.getIntermissionEndsAt(), show.getAvailableAt(), prices);
    }

    private PublicShowResponse toPublicResponse(ScheduledShow show) {
        BigDecimal minPrice = showSeatPriceRepository.findAllByScheduledShowIdOrderBySeatTypeCodeAsc(show.getId())
                .stream().map(ShowSeatPrice::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        Theater theater = show.getAuditorium().getTheater();
        return new PublicShowResponse(show.getId(), show.getContentType(), show.getContentId(),
                show.getContentTitle(), theater.getName(), show.getAuditorium().getName(), theater.getCity(),
                show.getStartsAt(), show.getEndsAt(), minPrice,
                internalApiProperties.currency().toUpperCase(Locale.ROOT), null);
    }
}
