package com.movie.user_service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.movie.user_service.entity.AuthProvider;
import com.movie.user_service.entity.OAuthAccount;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.User;
import com.movie.user_service.repository.OAuthRepository;
import com.movie.user_service.repository.UserRepository;
import com.movie.user_service.exceptions.AlreadyExistsException;
import com.movie.user_service.exceptions.ResourceNotFoundException;
import com.movie.user_service.exceptions.UnverifiedAccountException;

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

        List<OAuthAccount> oauthAccounts =
                oAuthRepository.findByProviderSubjectAndAuthProvider(
                        providerSubject,
                        authProvider
                );

        if (!oauthAccounts.isEmpty()) {
            User oAuthuser = oauthAccounts.getFirst().getUser();
            
            User user = userRepository.findByIdWithRoleAndPrivilege(oAuthuser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                if (!user.getEmail().equals(email)) {
                        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
                        throw new AlreadyExistsException("Email Already exists");
                        }
                        user.setEmail(email);
                }
                return user;
        }

        // A first OAuth login must reuse a local account with the same verified
        // email. This keeps one account, its existing password, roles, and data.
        User user = userRepository.findByEmail(email)
                .orElseGet(() ->  {
                        User newUser = new User();
                        newUser.setUserName(name);
                        newUser.setEmail(email);
                        newUser.setPassword(null);
                        newUser.setRoles(roles);
                        newUser.setOauthAccounts(new ArrayList<>());
                        newUser.setEmailVerified(true);
                        return newUser;
                });
        

        OAuthAccount oauthAccount = new OAuthAccount();
        oauthAccount.setAuthProvider(authProvider);
        oauthAccount.setProviderSubject(providerSubject);
        oauthAccount.setUser(user);
        if(user.isEmailVerified())
                user.getOauthAccounts().add(oauthAccount);
        else{
                throw new UnverifiedAccountException("Verify email to attach OAuth2 accounts");
        }
        User savedUser = userRepository.save(user);

        return savedUser;
    }


}