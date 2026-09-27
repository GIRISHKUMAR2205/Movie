package com.movie.user_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.movie.user_service.dto.LoginDto;
import com.movie.user_service.dto.GatewayAuthResponseDto;
import com.movie.user_service.dto.SignupDto;
import com.movie.user_service.entity.CustomUserDetails;
import com.movie.user_service.entity.OAuthAccount;
import com.movie.user_service.entity.Privilege;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.User;
import com.movie.user_service.exceptions.AlreadyExistsException;
import com.movie.user_service.mapper.UserMapper;
import com.movie.user_service.repository.OAuthRepository;
import com.movie.user_service.repository.RoleRepository;
import com.movie.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuthRepository oAuthRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private UserService userService;


    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }


    // ============================================================
    // saveUser()
    // ============================================================

    @Test
    void saveUser_shouldCreateUserSuccessfully() {

        // Arrange
        SignupDto signupDto = new SignupDto();
        signupDto.setEmail("john@test.com");
        signupDto.setPassword("password");

        User user = new User();
        user.setUserName("John");
        user.setEmail("john@test.com");

        Role userRole = mock(Role.class);
        Privilege privilege = mock(Privilege.class);

        when(userRole.getRoleName())
                .thenReturn("ROLE_USER");

        when(privilege.getPrivilegeName())
                .thenReturn("USER_READ");

        when(userRole.getPrivileges())
                .thenReturn(Set.of(privilege));

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.empty());

        when(userMapper.fromSignupDto(signupDto))
                .thenReturn(user);

        when(passwordEncoder.encode("password"))
                .thenReturn("encoded-password");

        when(roleRepository.findByRoleName("ROLE_USER"))
                .thenReturn(userRole);

        when(jwtService.generateToken(any(Authentication.class)))
                .thenReturn("jwt-token");

        when(refreshTokenService.issue(user))
                .thenReturn("opaque-token");

        // Act
        GatewayAuthResponseDto result = userService.saveUser(signupDto);

        // Assert
        assertEquals("John", result.name());
        assertEquals("john@test.com", result.email());
        assertEquals(Set.of("ROLE_USER"), result.roles());
        assertEquals("jwt-token", result.accessToken());
        assertEquals("opaque-token", result.refreshToken());

        assertEquals("encoded-password", user.getPassword());

        assertEquals(
                Set.of(userRole),
                user.getRoles()
        );

        verify(userRepository).save(user);

        verify(passwordEncoder)
                .encode("password");

        verify(roleRepository)
                .findByRoleName("ROLE_USER");

        verify(jwtService)
                .generateToken(any(Authentication.class));

    }


    @Test
    void saveUser_shouldThrowExceptionWhenUserAlreadyExists() {

        // Arrange
        SignupDto signupDto = new SignupDto();
        signupDto.setEmail("john@test.com");

        User existingUser = new User();

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(existingUser));

        // Act + Assert
        assertThrows(
                AlreadyExistsException.class,
                () -> userService.saveUser(signupDto)
        );

        // Make sure nothing else happened
        verify(userMapper, never())
                .fromSignupDto(any());

        verify(userRepository, never())
                .save(any());

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(jwtService, never())
                .generateToken(any());
    }


    @Test
    void saveUser_shouldAddRoleAndPrivilegesToAuthentication() {

        // Arrange
        SignupDto signupDto = new SignupDto();
        signupDto.setEmail("john@test.com");
        signupDto.setPassword("password");

        User user = new User();

        Role role = mock(Role.class);
        Privilege privilege = mock(Privilege.class);

        when(role.getRoleName())
                .thenReturn("ROLE_USER");

        when(privilege.getPrivilegeName())
                .thenReturn("USER_READ");

        when(role.getPrivileges())
                .thenReturn(Set.of(privilege));

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        when(userMapper.fromSignupDto(signupDto))
                .thenReturn(user);

        when(passwordEncoder.encode("password"))
                .thenReturn("encoded-password");

        when(roleRepository.findByRoleName("ROLE_USER"))
                .thenReturn(role);

        when(jwtService.generateToken(any()))
                .thenReturn("token");

        // Act
        userService.saveUser(signupDto);

        // Assert
        verify(jwtService).generateToken(
                argThat(auth -> {

                    Set<String> authorities = auth.getAuthorities()
                            .stream()
                            .map(a -> a.getAuthority())
                            .collect(java.util.stream.Collectors.toSet());

                    return authorities.contains("ROLE_USER")
                            && authorities.contains("USER_READ");
                })
        );
    }


    // ============================================================
    // loggedIn()
    // ============================================================

    @Test
    void loggedIn_shouldAuthenticateAndReturnToken() {

        // Arrange
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("john@test.com");
        loginDto.setPassword("password");

        CustomUserDetails user =
                mock(CustomUserDetails.class);

        when(user.getUserName())
                .thenReturn("John");

        when(user.getUsername())
                .thenReturn("john@test.com");

        when(user.getRoles())
                .thenReturn(Set.of());

        User persistedUser = new User();
        persistedUser.setEmail("john@test.com");
        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(persistedUser));

        when(authentication.getPrincipal())
                .thenReturn(user);

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtService.generateToken(authentication))
                .thenReturn("jwt-token");

        // Act
        GatewayAuthResponseDto result = userService.loggedIn(loginDto);

        // Assert
        assertNotNull(result);

        verify(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        verify(jwtService)
                .generateToken(authentication);
    }


    @Test
    void loggedIn_shouldThrowWhenAuthenticationFails() {

        // Arrange
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("john@test.com");
        loginDto.setPassword("wrong-password");

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act + Assert
        assertThrows(
                BadCredentialsException.class,
                () -> userService.loggedIn(loginDto)
        );

        verify(jwtService, never())
                .generateToken(any());
    }


    // ============================================================
    // getCurrentUser()
    // ============================================================

    @Test
    void getCurrentUser_shouldReturnBearerUser() {

        // Arrange
        User user = new User();
        user.setUserName("John");
        user.setEmail("john@test.com");

        when(authentication.getName()).thenReturn("john@test.com");

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));

        // Act
        GatewayAuthResponseDto result = userService.getCurrentUser();

        // Assert
        assertNotNull(result);

        verify(userRepository).findByEmail("john@test.com");
    }


    @Test
    void getCurrentUser_shouldReturnNormalUser() {

        // Arrange
        User user = new User();
        user.setUserName("John");
        user.setEmail("john@test.com");

        when(authentication.getName()).thenReturn("john@test.com");

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(user));

        // Act
        GatewayAuthResponseDto result = userService.getCurrentUser();

        // Assert
        assertNotNull(result);

        verify(userRepository)
                .findByEmail("john@test.com");
    }


    @Test
    void getCurrentUser_shouldThrowWhenUserNotFound() {

        // Arrange
        when(authentication.getName()).thenReturn("john@test.com");

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                RuntimeException.class,
                () -> userService.getCurrentUser()
        );
    }


    @Test
    void getCurrentUser_shouldThrowWhenNoAuthentication() {

        // Arrange
        when(securityContext.getAuthentication())
                .thenReturn(null);

        SecurityContextHolder.setContext(securityContext);

        // Act + Assert
        assertThrows(
                BadCredentialsException.class,
                () -> userService.getCurrentUser()
        );
    }
}
