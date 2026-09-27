package com.movie.apigateway.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";
    private static final Pattern VALID_CORRELATION_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = correlationIdFor(request.getHeader(CORRELATION_ID_HEADER));
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        MDC.put(MDC_KEY, correlationId);

        try {
            filterChain.doFilter(new CorrelationIdRequestWrapper(request, correlationId), response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String correlationIdFor(String suppliedCorrelationId) {
        if (suppliedCorrelationId != null && VALID_CORRELATION_ID.matcher(suppliedCorrelationId).matches()) {
            return suppliedCorrelationId;
        }
        return UUID.randomUUID().toString();
    }

    private static final class CorrelationIdRequestWrapper extends HttpServletRequestWrapper {

        private final String correlationId;

        private CorrelationIdRequestWrapper(HttpServletRequest request, String correlationId) {
            super(request);
            this.correlationId = correlationId;
        }

        @Override
        public String getHeader(String name) {
            return CORRELATION_ID_HEADER.equalsIgnoreCase(name) ? correlationId : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return CORRELATION_ID_HEADER.equalsIgnoreCase(name)
                    ? Collections.enumeration(List.of(correlationId))
                    : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> headerNames = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
            headerNames.removeIf(name -> CORRELATION_ID_HEADER.equalsIgnoreCase(name));
            headerNames.add(CORRELATION_ID_HEADER);
            return Collections.enumeration(headerNames);
        }
    }
}
