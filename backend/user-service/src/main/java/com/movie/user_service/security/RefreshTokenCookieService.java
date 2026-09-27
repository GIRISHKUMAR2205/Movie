package com.movie.user_service.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.movie.user_service.entity.RefreshTokenProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** Writes the opaque refresh credential without ever exposing it to JavaScript. */
@Service
@RequiredArgsConstructor
public class RefreshTokenCookieService {
    private final RefreshTokenCookieProperties cookieProperties;
    private final RefreshTokenProperties refreshTokenProperties;

    public void add(HttpServletResponse response, String refreshToken) {
        response.addHeader("Set-Cookie", cookie(refreshToken, refreshTokenProperties.ttl()).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader("Set-Cookie", cookie("", Duration.ZERO).toString());
    }

    public String read(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieProperties.name().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieProperties.name(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .path(cookieProperties.path())
                .maxAge(maxAge);
        if (cookieProperties.domain() != null && !cookieProperties.domain().isBlank()) {
            builder.domain(cookieProperties.domain());
        }
        return builder.build();
    }
}
