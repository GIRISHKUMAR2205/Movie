package com.movie.user_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService{
    private final  JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${spring.security.jwt.secret}")
    private String secret;
    @Value("${spring.security.jwt.expiration}")
    private int expirationAfter;

    public String generateToken(Authentication authentication){
        List<String> roleNames = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        JwtClaimsSet claimsSet=JwtClaimsSet.builder().
        subject(authentication.getName())
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plus(expirationAfter,ChronoUnit.DAYS))
        .claim("roles", roleNames)
        // .claims( claims -> {
        //     claims.put("roles",roleNames);
        // })
        .build();
        var jwtEncoderParameters = JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claimsSet);
        return jwtEncoder.encode(jwtEncoderParameters).getTokenValue();
    }

    public Jwt verifyToken(String token){
        try{
            return jwtDecoder.decode(token);
        }catch(JwtException ex){
            throw new BadJwtException("Invalid Jwt Token");
        }
    }

}
