package com.movie.theater_service.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movie.theater_service.dto.AuditoriumResponse;
import com.movie.theater_service.dto.CreateAuditoriumRequest;
import com.movie.theater_service.dto.CreateSeatTypeRequest;
import com.movie.theater_service.dto.CreateSeatsRequest;
import com.movie.theater_service.dto.CreateTheaterRequest;
import com.movie.theater_service.dto.SeatRequest;
import com.movie.theater_service.dto.SeatTypeResponse;
import com.movie.theater_service.dto.TheaterResponse;
import com.movie.theater_service.entity.Auditorium;
import com.movie.theater_service.entity.Seat;
import com.movie.theater_service.entity.SeatType;
import com.movie.theater_service.entity.Theater;
import com.movie.theater_service.error.ConflictException;
import com.movie.theater_service.error.ResourceNotFoundException;
import com.movie.theater_service.repository.AuditoriumRepository;
import com.movie.theater_service.repository.SeatRepository;
import com.movie.theater_service.repository.SeatTypeRepository;
import com.movie.theater_service.repository.TheaterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TheaterAdministrationService {

    private final TheaterRepository theaterRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public TheaterResponse registerTheater(String ownerSubject, CreateTheaterRequest request) {
        ZoneId.of(request.timeZone());
        Theater theater = new Theater();
        theater.setOwnerSubject(ownerSubject);
        theater.setName(request.name().trim());
        theater.setAddress(request.address().trim());
        theater.setCity(request.city().trim());
        theater.setCountry(request.country().trim());
        theater.setTimeZone(request.timeZone());
        return toResponse(theaterRepository.save(theater));
    }

    @Transactional(readOnly = true)
    public List<TheaterResponse> myTheaters(String ownerSubject) {
        return theaterRepository.findAllByOwnerSubjectOrderByNameAsc(ownerSubject).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public AuditoriumResponse addAuditorium(String ownerSubject, Long theaterId, CreateAuditoriumRequest request) {
        Theater theater = ownedTheater(ownerSubject, theaterId);
        if (auditoriumRepository.existsByTheaterIdAndNameIgnoreCase(theaterId, request.name().trim())) {
            throw new ConflictException("An auditorium with this name already exists in the theater.");
        }
        Auditorium auditorium = new Auditorium();
        auditorium.setTheater(theater);
        auditorium.setName(request.name().trim());
        return toResponse(auditoriumRepository.save(auditorium));
    }

    @Transactional(readOnly = true)
    public List<AuditoriumResponse> auditoriums(String ownerSubject, Long theaterId) {
        ownedTheater(ownerSubject, theaterId);
        return auditoriumRepository.findAllByTheaterIdOrderByNameAsc(theaterId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SeatTypeResponse addSeatType(String ownerSubject, Long theaterId, Long auditoriumId,
            CreateSeatTypeRequest request) {
        Auditorium auditorium = ownedAuditorium(ownerSubject, theaterId, auditoriumId);
        String code = normalizedCode(request.code());
        if (seatTypeRepository.existsByAuditoriumIdAndCodeIgnoreCase(auditoriumId, code)) {
            throw new ConflictException("A seat type with this code already exists in the auditorium.");
        }
        SeatType seatType = new SeatType();
        seatType.setAuditorium(auditorium);
        seatType.setCode(code);
        seatType.setDisplayName(request.displayName().trim());
        seatType.setDefaultPrice(money(request.defaultPrice()));
        return toResponse(seatTypeRepository.save(seatType));
    }

    @Transactional(readOnly = true)
    public List<SeatTypeResponse> seatTypes(String ownerSubject, Long theaterId, Long auditoriumId) {
        ownedAuditorium(ownerSubject, theaterId, auditoriumId);
        return seatTypeRepository.findAllByAuditoriumIdOrderByCodeAsc(auditoriumId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public int addSeats(String ownerSubject, Long theaterId, Long auditoriumId, CreateSeatsRequest request) {
        Auditorium auditorium = ownedAuditorium(ownerSubject, theaterId, auditoriumId);
        Map<String, SeatType> seatTypes = new HashMap<>();
        for (SeatType type : seatTypeRepository.findAllByAuditoriumIdOrderByCodeAsc(auditoriumId)) {
            seatTypes.put(type.getCode(), type);
        }
        Set<String> seatLocations = new HashSet<>();
        for (SeatRequest seatRequest : request.seats()) {
            String typeCode = normalizedCode(seatRequest.seatTypeCode());
            if (!seatTypes.containsKey(typeCode)) {
                throw new IllegalArgumentException("Unknown seat type: " + typeCode);
            }
            if (seatRepository.existsByAuditoriumIdAndRowLabelAndSeatNumber(
                    auditoriumId, seatRequest.rowLabel().trim(), seatRequest.seatNumber())) {
                throw new ConflictException("Seat " + seatRequest.rowLabel() + seatRequest.seatNumber() + " already exists.");
            }
            if (!seatLocations.add(seatRequest.rowLabel().trim() + "#" + seatRequest.seatNumber())) {
                throw new IllegalArgumentException("The request contains the same seat more than once.");
            }
        }
        List<Seat> seats = request.seats().stream().map(requestSeat -> {
            Seat seat = new Seat();
            seat.setAuditorium(auditorium);
            seat.setSeatType(seatTypes.get(normalizedCode(requestSeat.seatTypeCode())));
            seat.setRowLabel(requestSeat.rowLabel().trim());
            seat.setSeatNumber(requestSeat.seatNumber());
            return seat;
        }).toList();
        seatRepository.saveAll(seats);
        return seats.size();
    }

    Theater ownedTheater(String ownerSubject, Long theaterId) {
        return theaterRepository.findByIdAndOwnerSubject(theaterId, ownerSubject)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found."));
    }

    Auditorium ownedAuditorium(String ownerSubject, Long theaterId, Long auditoriumId) {
        ownedTheater(ownerSubject, theaterId);
        return auditoriumRepository.findByIdAndTheaterId(auditoriumId, theaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditorium not found."));
    }

    private String normalizedCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Prices must have at most two decimal places.");
        }
        return amount.setScale(2);
    }

    private TheaterResponse toResponse(Theater theater) {
        return new TheaterResponse(theater.getId(), theater.getName(), theater.getAddress(), theater.getCity(),
                theater.getCountry(), theater.getTimeZone());
    }

    private AuditoriumResponse toResponse(Auditorium auditorium) {
        return new AuditoriumResponse(auditorium.getId(), auditorium.getName());
    }

    private SeatTypeResponse toResponse(SeatType seatType) {
        return new SeatTypeResponse(seatType.getId(), seatType.getCode(), seatType.getDisplayName(),
                seatType.getDefaultPrice());
    }
}
