package com.movie.user_service.oauth2;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizationSuccessHandler;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.movie.user_service.security.CookieConfiguration;
import com.movie.user_service.security.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final CookieConfiguration cookieConfiguration;
    private static final Logger log=LoggerFactory.getLogger(OAuth2AuthorizationSuccessHandler.class);
    @Value("${frontend.app.oauth2-redirect-url}")
    private String frontEndRedirectUrl;
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();

        String token=jwtService.generateToken(authentication);
        Cookie cookie=cookieConfiguration.createCookie("auth-token",token,60*60*15);
        response.addCookie(cookie);
        log.info("email {}",user.getAttribute("email").toString());
        String targetUrl=UriComponentsBuilder.fromUriString(frontEndRedirectUrl).toUriString();
        response.sendRedirect(targetUrl);
    }
    
}
