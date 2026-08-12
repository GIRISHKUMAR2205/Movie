package com.movie.user_service.oauth2;

import org.springframework.stereotype.Service;

import com.movie.user_service.entity.AuthProvider;
import com.movie.user_service.entity.User;
import com.movie.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class OAuth2UserRegistrationService  {
    private final UserRepository userRepository;

    public  User processUserRegistration(String email,String name,String providerId,AuthProvider authProvider,String role){
        return userRepository.findByProviderIdAndAuthProvider(providerId, authProvider).orElseGet(()->{
            User user=new User();
            user.setUserName(name); 
            user.setEmail(email);
            user.setAuthProvider(authProvider);
            user.setProviderId(providerId);
            user.setRole(role);
            return userRepository.save(user);
        });
    }
}