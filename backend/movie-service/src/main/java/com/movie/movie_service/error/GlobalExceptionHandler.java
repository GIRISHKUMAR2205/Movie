package com.movie.movie_service.error;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> missing(ResourceNotFoundException error, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "MOVIE_NOT_FOUND", error.getMessage(), request);
    }

    @ExceptionHandler({ IllegalArgumentException.class, MethodArgumentNotValidException.class })
    ResponseEntity<ApiErrorResponse> invalid(Exception error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_MOVIE", "Movie details are invalid.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> conflict(DataIntegrityViolationException error, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "MOVIE_CONFLICT", "The movie conflicts with existing catalog data.", request);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), code, message, request.getRequestURI()));
    }
}
