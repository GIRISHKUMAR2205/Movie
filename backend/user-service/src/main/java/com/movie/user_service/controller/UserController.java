package com.movie.user_service.controller;

import com.movie.user_service.service.RoleRequestService;
import com.movie.user_service.service.EmailVerificationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.movie.user_service.dto.LoginDto;
import com.movie.user_service.dto.GatewayAuthResponseDto;
import com.movie.user_service.dto.GatewayResponseDto;
import com.movie.user_service.dto.RoleDto;
import com.movie.user_service.dto.RoleRequestCreatedDto;
import com.movie.user_service.dto.RoleRequestProgressDto;
import com.movie.user_service.dto.SignupDto;
import com.movie.user_service.service.UserService;
import com.movie.user_service.security.RefreshTokenCookieService;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class UserController {
    private static final String BROWSER_REQUEST_HEADER = "X-Requested-With";
    private static final String BROWSER_REQUEST_VALUE = "XMLHttpRequest";

    private final RoleRequestService roleRequestService;
    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    

    @PostMapping("/login")
    public ResponseEntity<GatewayResponseDto<GatewayAuthResponseDto>> login(
            @Valid @RequestBody LoginDto loginDto, HttpServletResponse response) {
        GatewayAuthResponseDto authResponse = userService.loggedIn(loginDto);
        refreshTokenCookieService.add(response, authResponse.refreshToken());
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "LOGIN_SUCCESS", "Login successful.", authResponse));
    }

    @PostMapping("/signup")
    public ResponseEntity<GatewayResponseDto<GatewayAuthResponseDto>> signup(
            @Valid @RequestBody SignupDto signupDto, HttpServletResponse response) {
        GatewayAuthResponseDto authResponse=userService.saveUser(signupDto);
        refreshTokenCookieService.add(response, authResponse.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(GatewayResponseDto.success(
                HttpStatus.CREATED, "SIGNUP_SUCCESS", "Account created successfully.", authResponse));
    }

    @PostMapping("/refresh")
    public ResponseEntity<GatewayResponseDto<GatewayAuthResponseDto>> refresh(
            @RequestHeader(value = BROWSER_REQUEST_HEADER, required = false) String requestedWith,
            HttpServletRequest request, HttpServletResponse response) {
        if (!BROWSER_REQUEST_VALUE.equals(requestedWith)) {
            throw new IllegalArgumentException("Refresh requires an XMLHttpRequest header.");
        }
        GatewayAuthResponseDto refreshed = userService.refresh(refreshTokenCookieService.read(request));
        refreshTokenCookieService.add(response, refreshed.refreshToken());
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "TOKEN_REFRESHED", "Tokens refreshed successfully.",
                refreshed));
    }


    @GetMapping("/me")
    public ResponseEntity<GatewayResponseDto<GatewayAuthResponseDto>> getUserName() {
        GatewayAuthResponseDto authResponse=userService.getCurrentUser();
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "CURRENT_USER_RETRIEVED", "Current user retrieved successfully.", authResponse));
    }

    @PostMapping("/signout")
    public ResponseEntity<GatewayResponseDto<Void>> logout(Authentication authentication, HttpServletResponse response) {
        userService.signOut(authentication.getName());
        refreshTokenCookieService.clear(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "LOGOUT_SUCCESS", "Signed out and revoked active refresh tokens.", null));
    }

    @PostMapping("/requestAdminPrivilege")
    public ResponseEntity<GatewayResponseDto<RoleRequestCreatedDto>> requestAdminPrivilege(
            @Valid @RequestBody RoleDto roleDto, Authentication auth){
        Long requestId = roleRequestService.request(auth.getName(), roleDto);
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "ROLE_REQUEST_CREATED", "Role request created successfully.",
                new RoleRequestCreatedDto(requestId)));
    }

    @GetMapping("/role-requests")
    public ResponseEntity<GatewayResponseDto<List<RoleRequestProgressDto>>> myRoleRequestProgress(Authentication auth) {
        return ResponseEntity.ok(GatewayResponseDto.success(HttpStatus.OK, "ROLE_REQUEST_PROGRESS_RETRIEVED",
                "Role request progress retrieved successfully.", roleRequestService.myRequestProgress(auth.getName())));
    }

    @GetMapping("/email-verifications/verify")
    public ResponseEntity<GatewayResponseDto<Void>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verify(token);
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "EMAIL_VERIFIED", "Email address verified successfully.", null));
    }

    @PostMapping("/email-verifications/resend")
    public ResponseEntity<GatewayResponseDto<Void>> resendVerification(Authentication auth) {
        emailVerificationService.resend(auth.getName());
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "VERIFICATION_EMAIL_SENT", "A verification email has been sent.", null));
    }
    
}
