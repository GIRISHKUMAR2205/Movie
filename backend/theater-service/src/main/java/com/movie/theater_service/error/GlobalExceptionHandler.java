package com.movie.theater_service.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InternalAuthenticationException.class)
    ResponseEntity<ApiErrorResponse> internalAuthentication(InternalAuthenticationException exception,
            HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "INTERNAL_AUTHENTICATION_FAILED", exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiErrorResponse> conflict(ConflictException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "SCHEDULE_CONFLICT", exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> dataConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "RESOURCE_CONFLICT",
                "The request conflicts with existing theater data or a show schedule.", request);
    }

    @ExceptionHandler({ IllegalArgumentException.class, MethodArgumentNotValidException.class })
    ResponseEntity<ApiErrorResponse> badRequest(Exception exception, HttpServletRequest request) {
        String message = exception instanceof MethodArgumentNotValidException validation
                ? validationMessage(validation) : exception.getMessage();
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message, request);
    }

    private String validationMessage(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return errors.toString();
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), code, message, request.getRequestURI()));
    }
}
