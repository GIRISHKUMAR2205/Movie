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
import com.movie.user_service.dto.RespDto;
import com.movie.user_service.dto.SignupDto;
import com.movie.user_service.entity.CustomUserDetails;
import com.movie.user_service.entity.OAuthAccount;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.User;
import com.movie.user_service.mapper.UserMapper;
import com.movie.user_service.repository.OAuthRepository;
import com.movie.user_service.repository.UserRepository;
import com.movie.user_service.exceptions.AlreadyExistsException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RoleRepository roleRepository;
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
            throw new AlreadyExistsException("UserName already exists");
        }
        log.info("signUPDTO {}",signupDto);
        User savedUser= userMapper.fromSignupDto(signupDto);
        savedUser.setPassword(passwordEncoder.encode(signupDto.getPassword()));
        Set<Role> roles=Set.of(roleRepository.findByRoleName("ROLE_USER"));
        savedUser.setRoles(roles);
        log.info("User saved {}",savedUser);
        savedUser.setCreatedBy(signupDto.getEmail());
        log.info("saveddUser {}", savedUser);
        userRepository.save(savedUser);

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
        RespDto respDto = userMapper.toRespDto(savedUser);
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
                user.getRoles());
    }

    public RespDto getCurrentUser() {
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        log.info("AUthentication {}",authentication);
        if(authentication != null ){
            String providerSubject = authentication.getPrincipal().toString();
            Optional<OAuthAccount> oauth = oAuthRepository.findByProviderSubject(providerSubject);
            User user;
            if (oauth.isPresent()) {
                user = oauth.get().getUser();
            } else {
                user = userRepository.findByEmail(authentication.getPrincipal().toString())
                        .orElseThrow(() -> new RuntimeException("User not found"));
            }

            return new RespDto(
                    user.getUserName(),
                    user.getEmail(),
                    null,//Change it to accessToken when implemented RefreshToken
                    user.getRoles());
        }else{
            throw new BadCredentialsException("No Active User");
        }
    }
   
}
