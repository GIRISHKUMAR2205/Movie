package com.movie.user_service.controller;

import com.movie.user_service.repository.UserRepository;
import com.movie.user_service.security.CookieConfiguration;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.movie.user_service.dto.UserDto;
import com.movie.user_service.entity.User;
import com.movie.user_service.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class UserController {

    private final OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CookieConfiguration cookieConfiguration;
    @Value("${frontend.app.base-url}")
    private String baseUrl;
    private static final Logger log=LoggerFactory.getLogger(UserController.class);


    // @GetMapping("/login/oauth2/code/google")
    // public ResponseEntity<?> getMethodName(Authentication auth) {
    //     OAuth2AuthorizedClient oAuth2AuthorizedClient=authorizedClientService.loadAuthorizedClient("google",auth.getName());
    //     OAuth2AccessToken oAuth2AccessToken=oAuth2AuthorizedClient.getAccessToken();
    //     log.info("{}",oAuth2AccessToken);
    //     return ResponseEntity.status(HttpStatus.OK).body(oAuth2AccessToken);
    // }
    

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDto userDto,HttpServletRequest request,HttpServletResponse response) {
        String token = userService.loggedIn(userDto);
        Cookie cookie=cookieConfiguration.createCookie("auth-token",token,60*60*15);
        
        response.addCookie(cookie);
        return ResponseEntity.status(HttpStatus.OK).body("User Logged In");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody UserDto userDto) {
        userService.saveUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("User Signed Up");
    }
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    @GetMapping("/me")
    public String getUserName() {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null)
            return auth.getName();
        else{
            throw new BadCredentialsException("No active user");
        }
    }
//Signout Perfectly Working But using Spring security logout
    @PostMapping("/signout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = cookieConfiguration.createCookie("auth-token","",0);
        response.addCookie(cookie);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.OK).body("User Logged Out");
    }

        @GetMapping("/signout/oidc")
    public void logoutOidc(HttpServletRequest request,HttpServletResponse response,Authentication authentication) throws IOException,ServletException {
        Cookie cookie = cookieConfiguration.createCookie("auth-token","",0);
        response.addCookie(cookie);
        // SecurityContextHolder.clearContext();
        // oidcLogoutSuccessHandler.onLogoutSuccess(request, response, authentication);
    }

    @DeleteMapping("/delete")
    public void delete(){
        userRepository.deleteAll();
    }
    
}
