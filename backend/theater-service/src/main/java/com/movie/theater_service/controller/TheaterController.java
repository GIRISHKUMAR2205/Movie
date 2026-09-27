package com.movie.theater_service.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.movie.theater_service.dto.AuditoriumResponse;
import com.movie.theater_service.dto.CreateAuditoriumRequest;
import com.movie.theater_service.dto.CreateSeatTypeRequest;
import com.movie.theater_service.dto.CreateSeatsRequest;
import com.movie.theater_service.dto.CreateShowScheduleRequest;
import com.movie.theater_service.dto.CreateTheaterRequest;
import com.movie.theater_service.dto.ScheduledShowResponse;
import com.movie.theater_service.dto.SeatTypeResponse;
import com.movie.theater_service.dto.TheaterResponse;
import com.movie.theater_service.dto.ShowInventoryResponse;
import com.movie.theater_service.dto.PublicShowResponse;
import com.movie.theater_service.service.ShowScheduleService;
import com.movie.theater_service.service.TheaterAdministrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class TheaterController {

    private final TheaterAdministrationService theaterAdministrationService;
    private final ShowScheduleService showScheduleService;

    @GetMapping("/shows")
    List<PublicShowResponse> publicShows(
            @RequestParam(required = false) String contentId,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate date) {
        return showScheduleService.publicShows(contentId, contentType, city, date);
    }

    @PostMapping
    ResponseEntity<TheaterResponse> register(Authentication authentication,
            @Valid @RequestBody CreateTheaterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(theaterAdministrationService.registerTheater(authentication.getName(), request));
    }

    @GetMapping("/mine")
    List<TheaterResponse> mine(Authentication authentication) {
        return theaterAdministrationService.myTheaters(authentication.getName());
    }

    @PostMapping("/{theaterId}/auditoriums")
    ResponseEntity<AuditoriumResponse> addAuditorium(Authentication authentication, @PathVariable Long theaterId,
            @Valid @RequestBody CreateAuditoriumRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(theaterAdministrationService.addAuditorium(authentication.getName(), theaterId, request));
    }

    @GetMapping("/{theaterId}/auditoriums")
    List<AuditoriumResponse> auditoriums(Authentication authentication, @PathVariable Long theaterId) {
        return theaterAdministrationService.auditoriums(authentication.getName(), theaterId);
    }

    @PostMapping("/{theaterId}/auditoriums/{auditoriumId}/seat-types")
    ResponseEntity<SeatTypeResponse> addSeatType(Authentication authentication, @PathVariable Long theaterId,
            @PathVariable Long auditoriumId, @Valid @RequestBody CreateSeatTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(theaterAdministrationService.addSeatType(
                authentication.getName(), theaterId, auditoriumId, request));
    }

    @GetMapping("/{theaterId}/auditoriums/{auditoriumId}/seat-types")
    List<SeatTypeResponse> seatTypes(Authentication authentication, @PathVariable Long theaterId,
            @PathVariable Long auditoriumId) {
        return theaterAdministrationService.seatTypes(authentication.getName(), theaterId, auditoriumId);
    }

    @PostMapping("/{theaterId}/auditoriums/{auditoriumId}/seats")
    ResponseEntity<Void> addSeats(Authentication authentication, @PathVariable Long theaterId,
            @PathVariable Long auditoriumId, @Valid @RequestBody CreateSeatsRequest request) {
        theaterAdministrationService.addSeats(authentication.getName(), theaterId, auditoriumId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{theaterId}/auditoriums/{auditoriumId}/show-schedules")
    ResponseEntity<List<ScheduledShowResponse>> schedule(Authentication authentication, @PathVariable Long theaterId,
            @PathVariable Long auditoriumId, @Valid @RequestBody CreateShowScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(showScheduleService.schedule(
                authentication.getName(), theaterId, auditoriumId, request));
    }

    @GetMapping("/{theaterId}/schedule")
    List<ScheduledShowResponse> schedule(@PathVariable Long theaterId, @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return showScheduleService.scheduleForTheater(theaterId, from, to);
    }

    @GetMapping("/internal/v1/shows/{showId}")
    ShowInventoryResponse bookingInventory(@PathVariable Long showId,
            @RequestHeader(value = "X-ShowHub-Internal-Key", required = false) String internalApiKey) {
        return showScheduleService.bookingInventory(showId, internalApiKey);
    }
}
