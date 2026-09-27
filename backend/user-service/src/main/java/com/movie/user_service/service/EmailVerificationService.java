package com.movie.user_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.movie.user_service.entity.EmailVerificationProperties;
import com.movie.user_service.entity.EmailVerificationToken;
import com.movie.user_service.entity.User;
import com.movie.user_service.exceptions.InvalidVerificationTokenException;
import com.movie.user_service.exceptions.ResourceNotFoundException;
import com.movie.user_service.repository.EmailVerificationTokenRepository;
import com.movie.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailVerificationProperties properties;
    private final EmailVerificationOutboxPublisher outboxPublisher;

    /** Generates a one-time link and sends it to the account's email address. */
    @Transactional
    public void sendVerificationEmail(User user) {
        tokenRepository.deleteByUserId(user.getId());
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(properties.tokenTtl()));
        tokenRepository.save(token);

        String verificationLink = UriComponentsBuilder.fromUriString(properties.verificationUrl())
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
        String subject = "Verify your ShowHub email address";
        String body = "Verify your email address by opening this link:\n\n" + verificationLink
                + "\n\nThis link expires in " + properties.tokenTtl().toHours() + " hours.";
        outboxPublisher.publish(user, token.getTokenHash(), verificationLink, subject, body);
    }

    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository
                .findByTokenHashAndConsumedAtIsNullAndExpiresAtAfter(hash(rawToken), Instant.now())
                .orElseThrow(InvalidVerificationTokenException::new);
        User user = token.getUser();
        user.setEmailVerified(true);
        token.setConsumedAt(Instant.now());
    }

    @Transactional
    public void resend(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isEmailVerified()) {
            sendVerificationEmail(user);
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }
}
