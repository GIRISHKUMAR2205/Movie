package com.movie.user_service.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.movie.user_service.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter{

    private final JwtService jwtService;


    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
                try{
                    String authorization = request.getHeader("Authorization");
                    if (authorization != null && authorization.startsWith("Bearer ")) {
                        String token = authorization.substring(7).trim();
                        if (!token.isEmpty()) {
                            Jwt jwt=jwtService.verifyAccessToken(token);
                            SecurityContext context=SecurityContextHolder.createEmptyContext();
                            Authentication auth=new UsernamePasswordAuthenticationToken(
                                    jwt.getSubject(), null,
                                    jwt.getClaimAsStringList("roles") == null ? List.of()
                                            : jwt.getClaimAsStringList("roles").stream()
                                                    .map(SimpleGrantedAuthority::new).toList());
                            context.setAuthentication(auth);
                            SecurityContextHolder.setContext(context);
                        }
                    }
                }catch(JwtException ex){
                    SecurityContextHolder.clearContext();
                }
                filterChain.doFilter(request, response);
    }
    
}
