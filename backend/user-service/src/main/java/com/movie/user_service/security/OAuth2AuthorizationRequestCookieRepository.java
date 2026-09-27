package com.movie.user_service.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Stores only the short-lived OAuth2 authorization request in a signed browser
 * cookie. This replaces Spring Security's HttpSession-backed repository, so
 * normal API authentication remains fully stateless.
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthorizationRequestCookieRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "showhub_oauth2_request";
    private static final int MAX_AGE_SECONDS = 300;
    private final JwtProperties jwtProperties;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String value = findCookie(request);
        if (value == null) {
            return null;
        }
        try {
            String[] parts = value.split("\\.", 2);
            if (parts.length != 2) {
                return null;
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            byte[] signature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(signature, sign(payload))) {
                return null;
            }
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(payload))) {
                Object requestObject = input.readObject();
                return requestObject instanceof OAuth2AuthorizationRequest authorizationRequest
                        ? authorizationRequest : null;
            }
        } catch (IOException | ClassNotFoundException | IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeCookie(response);
            return;
        }
        try {
            byte[] payload = serialize(authorizationRequest);
            String value = Base64.getUrlEncoder().withoutPadding().encodeToString(payload) + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
            if (value.length() > 3800) {
                throw new IllegalStateException("OAuth2 authorization request is too large for a cookie");
            }
            addCookie(response, value, MAX_AGE_SECONDS, request.isSecure());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to persist OAuth2 authorization request", exception);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        removeCookie(response);
        return authorizationRequest;
    }

    private byte[] serialize(OAuth2AuthorizationRequest authorizationRequest) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(authorizationRequest);
        }
        return bytes.toByteArray();
    }

    private byte[] sign(byte[] value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(jwtProperties.jwtSigningKey()), "HmacSHA256"));
            return mac.doFinal(value);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to sign OAuth2 authorization request", exception);
        }
    }

    private String findCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void removeCookie(HttpServletResponse response) {
        addCookie(response, "", 0, false);
    }

    private void addCookie(HttpServletResponse response, String value, int maxAge, boolean secure) {
        response.addHeader("Set-Cookie", ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(maxAge).build().toString());
    }
}
