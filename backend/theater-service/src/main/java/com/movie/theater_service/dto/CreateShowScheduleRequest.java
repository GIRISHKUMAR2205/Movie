package com.movie.theater_service.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateShowScheduleRequest(
        @NotNull ContentType contentType,
        @NotBlank @Size(max = 100) String contentId,
        @NotBlank @Size(max = 250) String contentTitle,
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate,
        @NotNull LocalTime showTime,
        @Min(1) @Max(600) int durationMinutes,
        @Min(0) Integer intermissionStartMinute,
        @Min(0) @Max(120) int intermissionDurationMinutes,
        @Min(0) @Max(240) int postShowBreakMinutes,
        List<@Valid ShowSeatPriceRequest> seatPrices) {

    public enum ContentType {
        MOVIE,
        CONCERT
    }
}
