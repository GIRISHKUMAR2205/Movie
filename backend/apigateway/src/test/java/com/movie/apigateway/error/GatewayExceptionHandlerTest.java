package com.movie.apigateway.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class GatewayExceptionHandlerTest {

    private final GatewayExceptionHandler exceptionHandler = new GatewayExceptionHandler();

    @Test
    void returnsTheStandardErrorShapeForExpectedRequestErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/movies/unknown");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie was not found"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .extracting(ApiErrorResponse::status, ApiErrorResponse::code, ApiErrorResponse::message,
                        ApiErrorResponse::path)
                .containsExactly(404, "GATEWAY_REQUEST_FAILED", "Movie was not found", "/api/v1/movies/unknown");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void doesNotExposeUnexpectedExceptionDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/bookings");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleUnexpected(
                new IllegalStateException("database password must not be returned"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .extracting(ApiErrorResponse::status, ApiErrorResponse::code, ApiErrorResponse::message,
                        ApiErrorResponse::path)
                .containsExactly(
                        500,
                        "GATEWAY_INTERNAL_ERROR",
                        "The gateway could not process the request.",
                        "/api/v1/bookings");
    }
}
