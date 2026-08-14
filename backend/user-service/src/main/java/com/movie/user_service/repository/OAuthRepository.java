package com.movie.user_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.user_service.entity.AuthProvider;
import com.movie.user_service.entity.OAuthAccount;

public interface OAuthRepository extends JpaRepository<OAuthAccount,Long>{

    List<OAuthAccount> findByProviderSubjectAndAuthProvider(String providerSubject, AuthProvider authProvider);

    OAuthAccount findByProviderSubject(String providerSubject);
    
} 
