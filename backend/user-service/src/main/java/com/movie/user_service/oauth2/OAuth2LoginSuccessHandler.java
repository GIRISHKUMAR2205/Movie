package com.movie.user_service.oauth2;

import com.movie.user_service.repository.UserRepository;
import com.movie.user_service.service.RoleRequestService;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.movie.user_service.dto.RoleDto;
import com.movie.user_service.entity.User;
import com.movie.user_service.security.CookieConfiguration;
import com.movie.user_service.service.JwtService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRequestService roleRequestService;
    private final JwtService jwtService;
    private final CookieConfiguration cookieConfiguration;

    @Value("${frontend.app.oauth2-redirect-url}")
    private String frontEndRedirectUrl;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();

        String token=jwtService.generateToken(authentication);
        Cookie cookie=cookieConfiguration.createCookie("auth-token",token,60*60*24*15);
        response.addCookie(cookie);


        String flow = (String) request.getSession().getAttribute("SIGNUP_FLOW");

        RoleDto roleDto=new RoleDto();
        if ("admin".equals(flow)) {
            User tempUser= userRepository.findByEmail(user.getAttribute("email").toString()).orElseThrow(() -> new UsernameNotFoundException("Will never happen"));
            roleDto.setUserId(tempUser.getId());
            roleDto.setRoleName("ROLE_ADMIN");
            
            roleRequestService.request(roleDto);
        } else if ("user".equals(flow)) {
            //Do Nothing for now just placeholder
        }

        String targetUrl=UriComponentsBuilder.fromUriString(frontEndRedirectUrl).toUriString();
        response.sendRedirect(targetUrl);
    }
    
}
