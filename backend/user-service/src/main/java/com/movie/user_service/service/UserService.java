package com.movie.user_service.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityReturnValueHandler;

import com.movie.user_service.dto.UserDto;
import com.movie.user_service.entity.User;
import com.movie.user_service.mapper.UserMapper;
import com.movie.user_service.repository.UserRepository;
import com.movie.user_service.security.JwtService;
import com.movie.user_service.exceptions.UserAlreadyExistsException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    

    public void saveUser(UserDto userDto) {
        Optional<User> user=userRepository.findByEmail(userDto.getEmail());
        
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        if(user.isPresent()){
            throw new UserAlreadyExistsException("UserName already exists");
        }
        userRepository.save(userMapper.toUser(userDto));
        
    }
    public String loggedIn(UserDto userDto){
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                userDto.getEmail(),userDto.getPassword()));
        
        return jwtService.generateToken(authentication);
    }
}
