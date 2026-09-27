package com.movie.user_service.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.movie.user_service.entity.CustomUserDetails;
import com.movie.user_service.entity.User;
import com.movie.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmailOrUserName(identifier, identifier)
                .orElseThrow(() ->
                    new UsernameNotFoundException("User not found"));
        return new CustomUserDetails(user);
    }
}
