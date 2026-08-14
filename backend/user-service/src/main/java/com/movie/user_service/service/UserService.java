package com.movie.user_service.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityReturnValueHandler;

import com.movie.user_service.dto.LoginDto;
import com.movie.user_service.dto.RespDto;
import com.movie.user_service.dto.SignupDto;
import com.movie.user_service.entity.CustomUserDetails;
import com.movie.user_service.entity.OAuthAccount;
import com.movie.user_service.entity.User;
import com.movie.user_service.mapper.UserMapper;
import com.movie.user_service.repository.OAuthRepository;
import com.movie.user_service.repository.UserRepository;
import com.movie.user_service.exceptions.UserAlreadyExistsException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final OAuthRepository oAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public static final Logger log=LoggerFactory.getLogger(UserService.class);
    

    public RespDto saveUser(SignupDto signupDto) {
        Optional<User> user=userRepository.findByEmail(signupDto.getEmail());
        if(user.isPresent()){
            throw new UserAlreadyExistsException("UserName already exists");
        }
        User savedUser= userMapper.fromSignupDto(signupDto);
        log.info("User saved {}",savedUser);
        signupDto.setRole("ROLE_USER");
        String refreshToken = refreshTokenService.generateRefreshToken();
        String hashToken = refreshTokenService.hashToken(refreshToken);
        // savedUser.setRefreshTokenHash(hashToken);
        // savedUser.setRefreshTokenExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));

        Authentication authentication=new UsernamePasswordAuthenticationToken(
            savedUser.getEmail(),
             null,
           List.of( new SimpleGrantedAuthority(savedUser.getRole())));
        String accessToken = jwtService.generateToken(authentication);
        RespDto respDto = userMapper.toRespDto(savedUser);
        respDto.setToken(accessToken);
        respDto.setToken(accessToken);
        return respDto;
    }
    public RespDto loggedIn(LoginDto loginDto){
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginDto.getEmail(),loginDto.getPassword()));
        
                CustomUserDetails user =
            (CustomUserDetails) authentication.getPrincipal();

    String accessToken = jwtService.generateToken(authentication);

    return new RespDto(
            user.getUserName(),
            user.getUsername(),
            accessToken,
            user.getRole());
    }
    public RespDto getCurrentUser() {
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        log.info("AUthentication {}",authentication);
// return ;
        if(authentication != null ){
            String providerSubject = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
                OAuthAccount oauth = oAuthRepository.findByProviderSubject(providerSubject);
                User user = userRepository.findById(oauth.getUser().getId()).get();
                log.info("USeer {}",user);
                String accessToken = jwtService.generateToken(authentication);

            return new RespDto(
                    user.getUserName(),
                    user.getEmail(),
                    accessToken,
                    user.getRole());
        }else{
            throw new BadCredentialsException("No Active User");
        }
    }
}
