package com.movie.user_service.oauth2;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {
    private static final Logger log=LoggerFactory.getLogger(OAuth2LoginFailureHandler.class);
    @Value("${frontend.app.login-url}")
    private String frontEndRedirectUrl;
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
            log.error("OAuth2 login failed: {}", exception.getMessage(), exception);

        String redirectUrl =
                frontEndRedirectUrl + "?error=google_failed";

        response.sendRedirect(redirectUrl);
    }
    
}
