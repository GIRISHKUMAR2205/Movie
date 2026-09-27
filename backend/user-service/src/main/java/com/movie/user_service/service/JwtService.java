package com.movie.user_service.service;

import java.time.Instant;
import java.util.List;

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
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import com.movie.user_service.security.JwtProperties;

@Service
@RequiredArgsConstructor
public class JwtService{
    private final  JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    public String generateToken(Authentication authentication){
        return generateAccessToken(authentication);
    }

    public String generateAccessToken(Authentication authentication){
        List<String> roleNames = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        Instant issuedAt = Instant.now();
        JwtClaimsSet claimsSet=JwtClaimsSet.builder()
        .issuer(jwtProperties.issuer())
        .audience(List.of(jwtProperties.audience()))
        .subject(authentication.getName())
        .issuedAt(issuedAt)
        .expiresAt(issuedAt.plus(jwtProperties.accessTokenTtl()))
        .claim("roles", roleNames)
        .claim("token_type", "access")
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

    public Jwt verifyAccessToken(String token) {
        return verifyTokenType(token, "access");
    }

    private Jwt verifyTokenType(String token, String expectedType) {
        Jwt jwt = verifyToken(token);
        if (!expectedType.equals(jwt.getClaimAsString("token_type"))) {
            throw new BadJwtException("Incorrect JWT token type");
        }
        return jwt;
    }

}
