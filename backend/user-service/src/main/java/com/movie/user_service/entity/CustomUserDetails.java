package com.movie.user_service.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;

@Data
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String userName;
    private final Set<Role> roles;
    // private final String refreshToken;
    // private final Instant refreshTokenExpiresAt;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.userName = user.getUserName();
        this.roles = user.getRoles();
        // this.refreshToken=user.getRefreshTokenHash();
        // this.refreshTokenExpiresAt=user.getRefreshTokenExpiresAt();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Set<GrantedAuthority> authorities = new HashSet<>();

        roles.forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role.getRoleName()));

            role.getPrivileges().forEach(privilege ->
                    authorities.add(
                            new SimpleGrantedAuthority(
                                    privilege.getPrivilegeName()
                            )
                    )
            );
        });

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public String getUserName() {
        return userName;
    }
    
}