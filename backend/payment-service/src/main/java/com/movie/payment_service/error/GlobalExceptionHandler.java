package com.movie.payment_service.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentNotFoundException.class)
    ResponseEntity<ApiErrorResponse> missing(PaymentNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", exception.getMessage(), request, null);
    }

    @ExceptionHandler({ PaymentConflictException.class, DataIntegrityViolationException.class,
            ObjectOptimisticLockingFailureException.class })
    ResponseEntity<ApiErrorResponse> conflict(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "PAYMENT_CONFLICT", exception instanceof PaymentConflictException
                ? exception.getMessage() : "The payment was changed concurrently or conflicts with existing data.", request, null);
    }

    @ExceptionHandler(InvalidWebhookException.class)
    ResponseEntity<ApiErrorResponse> invalidWebhook(InvalidWebhookException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK", exception.getMessage(), request, null);
    }

    @ExceptionHandler(PaymentDependencyException.class)
    ResponseEntity<ApiErrorResponse> dependency(PaymentDependencyException exception, HttpServletRequest request) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "BOOKING_SERVICE_UNAVAILABLE",
                exception.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> invalidRequest(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Payment request validation failed.", request, errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> invalidRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request, null);
    }

    @ExceptionHandler({ MissingRequestHeaderException.class, HttpMessageNotReadableException.class })
    ResponseEntity<ApiErrorResponse> malformedRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "A required header or request value is missing or malformed.", request, null);
    }

    private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message,
            HttpServletRequest request, Map<String, String> errors) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                Instant.now(), status.value(), code, message, request.getRequestURI(), errors));
    }
}
