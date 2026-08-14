package com.movie.user_service.security;

import java.io.IOException;
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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter{

    private final JwtService jwtService;
    private final CookieConfiguration cookieConfiguration;


    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
                try{
                    Cookie[] cookies=request.getCookies();
                    if(cookies != null){
                        for(Cookie cookie:cookies){
                            if(("JSESSIONID").equals(cookie.getName())){
                                Cookie jsessionCookie = cookieConfiguration.createCookie("JSESSIONID", "", 0);
                                response.addCookie(jsessionCookie);
                            }
                            else if(("auth-token").equals(cookie.getName())){
                                String token=cookie.getValue();
                                Jwt jwt=jwtService.verifyToken(token);
                                SecurityContext context=SecurityContextHolder.createEmptyContext();
                                Authentication auth=new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, jwt.getClaimAsStringList("roles").stream().map(SimpleGrantedAuthority::new).toList());
                                context.setAuthentication(auth);
                                SecurityContextHolder.setContext(context);
                            }
                        }
                    }
                }catch(JwtException ex){
                    System.out.print(ex);
                    SecurityContextHolder.clearContext();
                }
                filterChain.doFilter(request, response);
    }
    
}
