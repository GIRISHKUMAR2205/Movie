package com.movie.theater_service.dto;

public record TheaterResponse(Long id, String name, String address, String city, String country, String timeZone) {
}
