package com.movie.user_service.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthSignUpFilter extends OncePerRequestFilter {

    private static final String SIGNUP_FLOW = "SIGNUP_FLOW";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if ("/users/oauth2/authorization/google"
                .equals(request.getRequestURI())) {

            String flow = request.getParameter("flow");
             if (!Set.of(
                        "user",
                        "admin"
                ).contains(flow)) {

                    response.sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid signup flow"
                    );
                    return;
                }
            if (flow != null) {
                request.getSession()
                        .setAttribute(SIGNUP_FLOW, flow);
            }
        }

        filterChain.doFilter(request, response);
    }
}