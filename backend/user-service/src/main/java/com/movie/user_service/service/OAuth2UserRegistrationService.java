package com.movie.user_service.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.movie.user_service.entity.AuthProvider;
import com.movie.user_service.entity.OAuthAccount;
import com.movie.user_service.entity.User;
import com.movie.user_service.repository.OAuthRepository;
import com.movie.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class OAuth2UserRegistrationService  {
    private final UserRepository userRepository;
    private final OAuthRepository oAuthRepository;

    public  User processUserRegistration(String email,String name,String providerSubject,AuthProvider authProvider,String role){
        return userRepository.findByEmail(email).orElseGet(()->{
            User user=new User();
            user.setUserName(name); 
            user.setEmail(email);
            user.setRole(role);
            List<OAuthAccount> oAuthAccountList=oAuthRepository.findByProviderSubjectAndAuthProvider(providerSubject, authProvider);
            if(oAuthAccountList.isEmpty()){
                OAuthAccount newOAuthAccount =new OAuthAccount();
                newOAuthAccount.setAuthProvider(authProvider);
                newOAuthAccount.setUser(user);
                newOAuthAccount.setProviderSubject(providerSubject);
                oAuthAccountList.add(newOAuthAccount);
            }
            user.setOauthAccounts(oAuthAccountList);
            return userRepository.save(user);
        });
    }
}