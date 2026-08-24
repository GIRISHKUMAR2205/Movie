package com.movie.user_service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.movie.user_service.entity.AuthProvider;
import com.movie.user_service.entity.OAuthAccount;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.User;
import com.movie.user_service.repository.OAuthRepository;
import com.movie.user_service.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class OAuth2UserRegistrationService  {
    private final UserRepository userRepository;
    private final OAuthRepository oAuthRepository;

    @Transactional
    public User processUserRegistration(
            String email,
            String name,
            String providerSubject,
            AuthProvider authProvider,
            Set<Role> roles) {

        // 1. Check whether this OAuth account already exists
        List<OAuthAccount> oauthAccounts =
                oAuthRepository.findByProviderSubjectAndAuthProvider(
                        providerSubject,
                        authProvider
                );

        if (!oauthAccounts.isEmpty()) {
            User user = oauthAccounts.get(0).getUser();

            return userRepository.findByIdWithRoleAndPrivilege(user.getId())
                    .orElseThrow(() ->
                            new RuntimeException ("User not found"));
        }

        // 2. OAuth account does not exist.
        //    Create a NEW user.
        User user = new User();
        user.setUserName(name);
        user.setEmail(email);
        user.setPassword(null);
        user.setRoles(roles);

        OAuthAccount oauthAccount = new OAuthAccount();
        oauthAccount.setAuthProvider(authProvider);
        oauthAccount.setProviderSubject(providerSubject);
        oauthAccount.setUser(user);

        user.setOauthAccounts(new ArrayList<>());
        user.getOauthAccounts().add(oauthAccount);

        userRepository.save(user);

        

        // 3. Fetch everything required for authorization
        return userRepository.findByIdWithRoleAndPrivilege(user.getId())
                .orElseThrow(() ->
                        new RuntimeException("User not found after registration"));
        }
}