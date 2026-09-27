package com.movie.user_service.security;

import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.movie.user_service.dto.GatewayResponseDto;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuthSignUpFilter extends OncePerRequestFilter {

    public static final String SIGNUP_FLOW_COOKIE = "showhub_oauth_signup_flow";
    private final ObjectMapper objectMapper;

    public OAuthSignUpFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if ("/users/oauth2/authorization/google"
                .equals(request.getRequestURI())) {

            String flow = request.getParameter("flow");
            if (!"user".equals(flow) && !"admin".equals(flow)) {

                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getOutputStream(), GatewayResponseDto.failure(
                            HttpStatus.BAD_REQUEST,
                            "INVALID_SIGNUP_FLOW",
                            "Invalid signup flow."));
                    return;
                }
            response.addHeader("Set-Cookie", ResponseCookie.from(SIGNUP_FLOW_COOKIE, flow)
                    .httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/").maxAge(300).build().toString());
        }

        filterChain.doFilter(request, response);
    }
}
