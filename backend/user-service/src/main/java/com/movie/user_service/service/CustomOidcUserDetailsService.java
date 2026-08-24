package com.movie.user_service.service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import com.movie.user_service.entity.AuthProvider;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.User;
import com.movie.user_service.repository.RoleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOidcUserDetailsService  extends OidcUserService{
    private final OAuth2UserRegistrationService registrationService;
    private final RoleRepository roleRepository;

        public OidcUser loadUser(OidcUserRequest userRequest){
                OidcUser oidcUser = super.loadUser(userRequest);
                String registrationId=userRequest.getClientRegistration().getRegistrationId();
                AuthProvider authProvider = AuthProvider.getAuthProvider(registrationId);

                Set<Role> role=Set.of(roleRepository.findByRoleName("ROLE_USER"));

                User dbUser=registrationService.processUserRegistration(oidcUser.getEmail(), oidcUser.getClaims().get("name").toString(),oidcUser.getSubject(),authProvider,role);           
                Set<GrantedAuthority> mappedAuthorities = new HashSet<>(oidcUser.getAuthorities());
                mappedAuthorities.addAll(dbUser.getRoles()
        .stream()
        .flatMap(roles -> {
            Stream<SimpleGrantedAuthority> roleAuthority =
                    Stream.of(new SimpleGrantedAuthority(roles.getRoleName()));

            Stream<SimpleGrantedAuthority> privilegeAuthorities =
                    roles.getPrivileges()
                            .stream()
                            .map(privilege ->
                                    new SimpleGrantedAuthority(
                                            privilege.getPrivilegeName()));

            return Stream.concat(roleAuthority, privilegeAuthorities);
        })
        .collect(Collectors.toSet()));        
                
                oidcUser = new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());

                return oidcUser;
        };
}

