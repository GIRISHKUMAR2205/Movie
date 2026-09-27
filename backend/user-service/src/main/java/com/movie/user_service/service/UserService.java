package com.movie.user_service.service;

import com.movie.user_service.repository.RoleRepository;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.movie.user_service.dto.LoginDto;
import com.movie.user_service.dto.GatewayAuthResponseDto;
import com.movie.user_service.dto.SignupDto;
import com.movie.user_service.entity.CustomUserDetails;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.User;
import com.movie.user_service.mapper.UserMapper;
import com.movie.user_service.repository.UserRepository;
import com.movie.user_service.exceptions.AlreadyExistsException;
import com.movie.user_service.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;

    
    public static final Logger log=LoggerFactory.getLogger(UserService.class);


    

    @Transactional
    public GatewayAuthResponseDto saveUser(SignupDto signupDto) {
        Optional<User> user=userRepository.findByEmail(signupDto.getEmail());
        if(user.isPresent()){
            throw new AlreadyExistsException("UserName already exists");
        }

        User savedUser= userMapper.fromSignupDto(signupDto);
        savedUser.setPassword(passwordEncoder.encode(signupDto.getPassword()));
        Set<Role> roles=Set.of(roleRepository.findByRoleName("ROLE_USER"));
        savedUser.setRoles(roles);
        savedUser.setCreatedBy(signupDto.getEmail());
        userRepository.save(savedUser);
        emailVerificationService.sendVerificationEmail(savedUser);

        Set<SimpleGrantedAuthority> authorities = savedUser.getRoles()
        .stream()
        .flatMap(role -> {
            Stream<SimpleGrantedAuthority> roleAuthority =
                    Stream.of(new SimpleGrantedAuthority(role.getRoleName()));

            Stream<SimpleGrantedAuthority> privilegeAuthorities =
                    role.getPrivileges()
                            .stream()
                            .map(privilege ->
                                    new SimpleGrantedAuthority(
                                            privilege.getPrivilegeName()));

            return Stream.concat(roleAuthority, privilegeAuthorities);
        })
        .collect(Collectors.toSet());
        Authentication authentication=new UsernamePasswordAuthenticationToken(
            savedUser.getEmail(),
             null,
             authorities);
        String accessToken = jwtService.generateToken(authentication);
        return GatewayAuthResponseDto.from(savedUser, accessToken, refreshTokenService.issue(savedUser));
    }

    public GatewayAuthResponseDto loggedIn(LoginDto loginDto){
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(),loginDto.getPassword()));
        
                CustomUserDetails user =
            (CustomUserDetails) authentication.getPrincipal();

        String accessToken = jwtService.generateToken(authentication);
        User persistedUser = userRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        String refreshToken = refreshTokenService.issue(persistedUser);

        return GatewayAuthResponseDto.from(user.getUserName(), user.getUsername(), user.getRoles(),
                persistedUser.isEmailVerified(), accessToken, refreshToken);
    }

    public GatewayAuthResponseDto getCurrentUser() {
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null ){
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            return GatewayAuthResponseDto.from(user, null, null);
        }else{
            throw new BadCredentialsException("No Active User");
        }
    }

    @Transactional
    public GatewayAuthResponseDto refresh(String refreshToken) {
        RefreshTokenService.RotatedRefreshToken rotatedToken = refreshTokenService.rotate(refreshToken);
        User user = rotatedToken.user();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, authoritiesFor(user));
        return GatewayAuthResponseDto.from(user, jwtService.generateAccessToken(authentication),
                rotatedToken.rawToken());
    }

    @Transactional
    public void signOut(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenService.revokeAllForUser(user);
    }

    private Set<SimpleGrantedAuthority> authoritiesFor(User user) {
        return user.getRoles().stream()
                .flatMap(role -> Stream.concat(
                        Stream.of(new SimpleGrantedAuthority(role.getRoleName())),
                        role.getPrivileges().stream().map(privilege ->
                                new SimpleGrantedAuthority(privilege.getPrivilegeName()))))
                .collect(Collectors.toSet());
    }
   
}
