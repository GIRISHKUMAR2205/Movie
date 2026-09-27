package com.movie.user_service.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.movie.user_service.dto.GatewayResponseDto;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<GatewayResponseDto<Void>> authFailed(BadCredentialsException ex){
        return failure(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
    }
    @ExceptionHandler(UsernameNotFoundException.class)
    ResponseEntity<GatewayResponseDto<Void>> notFound(UsernameNotFoundException ex){
        return failure(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "The requested user was not found.");
    }
    @ExceptionHandler(AlreadyExistsException.class)
    ResponseEntity<GatewayResponseDto<Void>> alreadyExists(AlreadyExistsException ex){
        return failure(HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS", ex.getMessage());
    }

    @ExceptionHandler(UnverifiedAccountException.class)
    ResponseEntity<GatewayResponseDto<Void>> unverifiedAccount(UnverifiedAccountException ex) {
        return failure(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", ex.getMessage());
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    ResponseEntity<GatewayResponseDto<Void>> invalidVerificationToken(InvalidVerificationTokenException ex) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN", ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<GatewayResponseDto<Void>> invalidRefreshToken(InvalidRefreshTokenException ex) {
        return failure(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", ex.getMessage());
    }

    @ExceptionHandler(InvalidRoleRequestStateException.class)
    ResponseEntity<GatewayResponseDto<Void>> invalidRoleRequestState(InvalidRoleRequestStateException ex) {
        return failure(HttpStatus.CONFLICT, "INVALID_ROLE_REQUEST_STATE", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<GatewayResponseDto<Void>> illegalArgument(IllegalArgumentException ex) {
        return failure(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<GatewayResponseDto<Void>> resourceNotFound(ResourceNotFoundException ex){
        return failure(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<GatewayResponseDto<Void>> validationFailed(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(GatewayResponseDto.failure(
                HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed.", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<GatewayResponseDto<Void>> unreadableRequest(HttpMessageNotReadableException ex) {
        return failure(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is malformed.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<GatewayResponseDto<Void>> dataIntegrityViolation(DataIntegrityViolationException ex) {
        return failure(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", "The request conflicts with existing data.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<GatewayResponseDto<Void>> unexpectedFailure(Exception ex, HttpServletRequest request) {
        log.error("Unhandled user-service error for {} {} ({})",
                request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName());
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "The request could not be processed.");
    }

    private ResponseEntity<GatewayResponseDto<Void>> failure(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(GatewayResponseDto.failure(status, code, message));
    }
}
