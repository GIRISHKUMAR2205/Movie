package com.movie.apigateway.error;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GatewayExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request) {
        HttpStatusCode status = exception.getStatusCode();
        String message = exception.getReason() == null ? status.toString() : exception.getReason();

        return ResponseEntity.status(status)
                .body(error(status, "GATEWAY_REQUEST_FAILED", message, request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, "ROUTE_NOT_FOUND", "The requested route does not exist.", request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error(
                "Unhandled gateway error for {} {} ({})",
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "GATEWAY_INTERNAL_ERROR",
                        "The gateway could not process the request.",
                        request));
    }

    private ApiErrorResponse error(HttpStatusCode status, String code, String message, HttpServletRequest request) {
        return new ApiErrorResponse(Instant.now(), status.value(), code, message, request.getRequestURI());
    }
}
