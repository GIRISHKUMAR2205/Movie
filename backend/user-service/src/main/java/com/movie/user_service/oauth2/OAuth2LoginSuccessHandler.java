package com.movie.user_service.oauth2;

import com.movie.user_service.repository.UserRepository;
import com.movie.user_service.service.RoleRequestService;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.movie.user_service.dto.RoleDto;
import com.movie.user_service.entity.User;
import com.movie.user_service.exceptions.AlreadyExistsException;
import com.movie.user_service.security.OAuthSignUpFilter;
import com.movie.user_service.security.RefreshTokenCookieService;
import com.movie.user_service.service.JwtService;
import com.movie.user_service.service.RefreshTokenService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRequestService roleRequestService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @Value("${frontend.app.oauth2-redirect-url}")
    private String frontEndRedirectUrl;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();

        User dbUser = userRepository.findByEmail(user.getAttribute("email"))
                .orElseThrow(() -> new UsernameNotFoundException("Authenticated user no longer exists"));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                dbUser.getEmail(), null, authentication.getAuthorities());
        String token=jwtService.generateToken(auth);
        refreshTokenCookieService.add(response, refreshTokenService.issue(dbUser));
        String flow = findCookie(request, OAuthSignUpFilter.SIGNUP_FLOW_COOKIE);
        response.addHeader("Set-Cookie", "showhub_oauth_signup_flow=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");

        RoleDto roleDto=new RoleDto();
        if ("admin".equals(flow)) {
            roleDto.setRoleName("ROLE_ADMIN");
            
            try {
                roleRequestService.request(dbUser.getEmail(), roleDto);
            } catch (AlreadyExistsException ignored) {
                // An existing admin role or pending request is already the desired state.
            }
        } else if ("user".equals(flow)) {
            //Do Nothing for now just placeholder
        }

        // Tokens are placed in the fragment so the browser does not send them to
        // the frontend server in an HTTP request. The SPA can retain it in memory
        // and use it as an Authorization: Bearer token.
        String targetUrl=UriComponentsBuilder.fromUriString(frontEndRedirectUrl)
                .fragment("access_token=" + token + "&token_type=Bearer")
                .build()
                .toUriString();
        response.sendRedirect(targetUrl);
    }

    private String findCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
    
}
