package com.movie.payment_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.movie.payment_service.config.PaymentProviderProperties;
import com.movie.payment_service.error.InvalidWebhookException;

@Component
public class WebhookSignatureVerifier {
    private final PaymentProviderProperties properties;
    private final Clock clock;

    public WebhookSignatureVerifier(PaymentProviderProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void verify(String timestampHeader, String signatureHeader, String rawBody) {
        if (timestampHeader == null || signatureHeader == null) {
            throw new InvalidWebhookException("Webhook signature headers are missing.");
        }
        long epochSeconds;
        try {
            epochSeconds = Long.parseLong(timestampHeader);
        } catch (NumberFormatException exception) {
            throw new InvalidWebhookException("Webhook timestamp is invalid.");
        }
        Duration age = Duration.between(Instant.ofEpochSecond(epochSeconds), clock.instant()).abs();
        if (age.compareTo(properties.webhookTolerance()) > 0) {
            throw new InvalidWebhookException("Webhook timestamp is outside the accepted window.");
        }
        byte[] supplied;
        try {
            supplied = HexFormat.of().parseHex(signatureHeader);
        } catch (IllegalArgumentException exception) {
            throw new InvalidWebhookException("Webhook signature is malformed.");
        }
        byte[] expected = hmac(timestampHeader + "." + rawBody);
        if (!MessageDigest.isEqual(expected, supplied)) {
            throw new InvalidWebhookException("Webhook signature is invalid.");
        }
    }

    public String payloadHash(String rawBody) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private byte[] hmac(String value) {
        try {
            if (properties.webhookSecret() == null || properties.webhookSecret().length() < 32) {
                throw new IllegalStateException("PAYMENT_WEBHOOK_SECRET must contain at least 32 characters.");
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify the webhook signature.", exception);
        }
    }
}
