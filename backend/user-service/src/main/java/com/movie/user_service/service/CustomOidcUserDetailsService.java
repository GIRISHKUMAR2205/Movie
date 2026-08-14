package com.movie.user_service.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.movie.user_service.entity.AuthProvider;
import com.movie.user_service.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOidcUserDetailsService  extends OidcUserService{
    private final OAuth2UserRegistrationService registrationService;
        public OidcUser loadUser(OidcUserRequest userRequest){
                OidcUser oidcUser = super.loadUser(userRequest);
                String registrationId=userRequest.getClientRegistration().getRegistrationId();
                AuthProvider authProvider = AuthProvider.getAuthProvider(registrationId);

                User dbUser=registrationService.processUserRegistration(oidcUser.getEmail(), oidcUser.getClaims().get("name").toString(),oidcUser.getSubject(),authProvider,"ROLE_USER");           
                Set<GrantedAuthority> mappedAuthorities = new HashSet<>(oidcUser.getAuthorities());
                mappedAuthorities.add(new SimpleGrantedAuthority(dbUser.getRole()));
                
                oidcUser = new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());

                return oidcUser;
        };
}

