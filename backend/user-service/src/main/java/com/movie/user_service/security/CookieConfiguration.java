package com.movie.user_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;

@Component
public class CookieConfiguration {
    @Value("${spring.security.cookie.httpOnly}")
    private boolean httpOnly;
    @Value("${spring.security.cookie.secure}")
    private boolean secure;
    public Cookie createCookie(String name,String value,int maxAge){
        Cookie cookie=new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(secure);
        cookie.setPath("/users"); //added users because of context servelet
        return cookie;
    }
}
