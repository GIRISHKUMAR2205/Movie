package com.movie.payment_service.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.movie.payment_service.config.PaymentProviderProperties;
import com.movie.payment_service.error.InvalidWebhookException;

class WebhookSignatureVerifierTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");
    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(
            new PaymentProviderProperties(SECRET, "http://localhost/checkout", Duration.ofMinutes(5)),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void acceptsAValidTimestampedSignature() throws Exception {
        String body = "{\"eventId\":\"evt-1\"}";
        String timestamp = String.valueOf(NOW.getEpochSecond());
        verifier.verify(timestamp, signature(timestamp, body), body);
    }

    @Test
    void rejectsExpiredAndTamperedCallbacks() throws Exception {
        String body = "{\"eventId\":\"evt-1\"}";
        String expiredTimestamp = String.valueOf(NOW.minus(Duration.ofMinutes(6)).getEpochSecond());
        assertThatThrownBy(() -> verifier.verify(expiredTimestamp, signature(expiredTimestamp, body), body))
                .isInstanceOf(InvalidWebhookException.class);

        String timestamp = String.valueOf(NOW.getEpochSecond());
        assertThatThrownBy(() -> verifier.verify(timestamp, signature(timestamp, body), body + " "))
                .isInstanceOf(InvalidWebhookException.class);
    }

    private String signature(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }
}
