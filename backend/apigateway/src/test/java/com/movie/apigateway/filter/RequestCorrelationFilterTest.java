package com.movie.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void propagatesASuppliedValidCorrelationIdToTheResponseAndDownstreamRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/movies");
        request.addHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<HttpServletRequest> downstreamRequest = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                downstreamRequest.set((HttpServletRequest) servletRequest));

        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER)).isEqualTo("request-123");
        assertThat(downstreamRequest.get().getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER))
                .isEqualTo("request-123");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void replacesAnInvalidCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/movies");
        request.addHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER, "contains a space");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> { });

        assertThat(response.getHeader(RequestCorrelationFilter.CORRELATION_ID_HEADER))
                .matches("[A-Za-z0-9._-]{1,128}")
                .isNotEqualTo("contains a space");
    }
}
