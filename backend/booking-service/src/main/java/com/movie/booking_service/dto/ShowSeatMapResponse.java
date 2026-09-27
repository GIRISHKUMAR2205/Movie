package com.movie.booking_service.dto;

import java.util.List;

public record ShowSeatMapResponse(Long showId, List<ShowSeatResponse> seats, long holdDurationSeconds) {
}
