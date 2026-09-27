package com.movie.user_service.dto;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Stable, gateway-facing authentication response. The client sends the returned
 * access token in an Authorization: Bearer header; it is never stored server-side.
 */
public record GatewayAuthResponseDto(
        String name,
        String email,
        Set<String> roles,
        boolean emailVerified,
        String accessToken,
        @JsonIgnore String refreshToken) {

    public static GatewayAuthResponseDto from(
            String name,
            String email,
            Set<Role> roles,
            boolean emailVerified,
            String accessToken,
            String refreshToken) {
        Set<String> roleNames = roles == null
                ? Set.of()
                : roles.stream()
                        .filter(Objects::nonNull)
                        .map(Role::getRoleName)
                        .collect(Collectors.toUnmodifiableSet());

        return new GatewayAuthResponseDto(
                name, email, roleNames, emailVerified, accessToken, refreshToken);
    }

    public static GatewayAuthResponseDto from(User user, String accessToken, String refreshToken) {
        return from(user.getUserName(), user.getEmail(), user.getRoles(), user.isEmailVerified(),
                accessToken, refreshToken);
    }
}
