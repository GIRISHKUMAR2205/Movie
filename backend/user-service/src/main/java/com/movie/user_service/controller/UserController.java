package com.movie.user_service.controller;

import com.movie.user_service.repository.UserRepository;
import com.movie.user_service.security.CookieConfiguration;

import com.movie.user_service.service.RoleRequestService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.movie.user_service.dto.LoginDto;
import com.movie.user_service.dto.RespDto;
import com.movie.user_service.dto.RoleDto;
import com.movie.user_service.dto.SignupDto;
import com.movie.user_service.entity.User;
import com.movie.user_service.service.UserService;

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
    private final RoleRequestService roleRequestService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CookieConfiguration cookieConfiguration;


    // @GetMapping("/login/oauth2/code/google")
    // public ResponseEntity<?> getMethodName(Authentication auth) {
    //     OAuth2AuthorizedClient oAuth2AuthorizedClient=authorizedClientService.loadAuthorizedClient("google",auth.getName());
    //     OAuth2AccessToken oAuth2AccessToken=oAuth2AuthorizedClient.getAccessToken();
    //     log.info("{}",oAuth2AccessToken);
    //     return ResponseEntity.status(HttpStatus.OK).body(oAuth2AccessToken);
    // }
    

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto,HttpServletRequest request,HttpServletResponse response) {
        RespDto respDto = userService.loggedIn(loginDto);
        Cookie cookie=cookieConfiguration.createCookie("auth-token",respDto.getToken(),60*60*24*15);
        respDto.setToken(null);
        response.addCookie(cookie);
        return ResponseEntity.status(HttpStatus.OK).body(respDto);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupDto signupDto,HttpServletResponse response) {
        RespDto respDto=userService.saveUser(signupDto);
        Cookie cookie=cookieConfiguration.createCookie("auth-token",respDto.getToken(),60*60*24*15);
        respDto.setToken(null);
        response.addCookie(cookie);
        return ResponseEntity.status(HttpStatus.CREATED).body(respDto);
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserName(HttpServletResponse response) {
        RespDto respDto=userService.getCurrentUser();
        respDto.setToken(null);
        return ResponseEntity.status(HttpStatus.OK).body(respDto);
    }

    //Signout Perfectly Working But using Spring security logout
    @PostMapping("/signout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = cookieConfiguration.createCookie("auth-token","",0);
        response.addCookie(cookie);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.OK).body("User Logged Out");
    }

    @DeleteMapping("/delete")
    public void delete(){
        userRepository.deleteAll();
    }

    @PostMapping("/requestAdminPrivilege")
    public ResponseEntity<?> requestAdminPrivilege(@RequestBody RoleDto roleDto){
        roleRequestService.request(roleDto);
        return ResponseEntity.status(HttpStatus.OK).body("Request raised successfully");
    }
    
}
