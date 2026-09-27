package com.movie.booking_service.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BookingNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(BookingNotFoundException error, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", error.getMessage(), request, null);
    }

    @ExceptionHandler({ BookingConflictException.class, DataIntegrityViolationException.class,
            ObjectOptimisticLockingFailureException.class })
    ResponseEntity<ApiErrorResponse> conflict(Exception error, HttpServletRequest request) {
        String message = error instanceof BookingConflictException ? error.getMessage()
                : "One or more seats were reserved concurrently. Refresh availability and try again.";
        return response(HttpStatus.CONFLICT, "BOOKING_CONFLICT", message, request, null);
    }

    @ExceptionHandler(DownstreamServiceException.class)
    ResponseEntity<ApiErrorResponse> unavailable(DownstreamServiceException error, HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "THEATER_SERVICE_UNAVAILABLE", error.getMessage(), request, null);
    }

    @ExceptionHandler(InternalAuthenticationException.class)
    ResponseEntity<ApiErrorResponse> internalAuth(InternalAuthenticationException error, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "INTERNAL_AUTHENTICATION_FAILED", error.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException error, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError field : error.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(field.getField(), field.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Booking request validation failed.", request, errors);
    }

    @ExceptionHandler({ IllegalArgumentException.class, MissingRequestHeaderException.class })
    ResponseEntity<ApiErrorResponse> invalid(Exception error, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", error.getMessage(), request, null);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message,
            HttpServletRequest request, Map<String, String> errors) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), code, message, request.getRequestURI(), errors));
    }
}
