package com.movie.payment_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.movie.payment_service.dto.CreatePaymentRequest;
import com.movie.payment_service.dto.PaymentResponse;
import com.movie.payment_service.dto.RefundRequest;
import com.movie.payment_service.dto.RefundResponse;
import com.movie.payment_service.dto.SandboxWebhookRequest;
import com.movie.payment_service.error.InvalidWebhookException;
import com.movie.payment_service.service.PaymentApplicationService;
import com.movie.payment_service.service.WebhookSignatureVerifier;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentApplicationService paymentService;
    private final WebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping
    ResponseEntity<PaymentResponse> create(Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.create(authentication.getName(), idempotencyKey, request));
    }

    @GetMapping
    List<PaymentResponse> list(Authentication authentication) {
        return paymentService.list(authentication.getName());
    }

    @GetMapping("/{paymentId}")
    PaymentResponse get(Authentication authentication, @PathVariable UUID paymentId) {
        return paymentService.get(authentication.getName(), paymentId);
    }

    @PostMapping("/{paymentId}/cancel")
    PaymentResponse cancel(Authentication authentication, @PathVariable UUID paymentId) {
        return paymentService.cancel(authentication.getName(), paymentId);
    }

    @PostMapping("/{paymentId}/refunds")
    ResponseEntity<RefundResponse> refund(Authentication authentication, @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.refund(authentication.getName(), paymentId, request));
    }

    @PostMapping("/webhooks/sandbox")
    PaymentResponse sandboxWebhook(@RequestHeader("X-ShowHub-Timestamp") String timestamp,
            @RequestHeader("X-ShowHub-Signature") String signature,
            @RequestBody String rawBody) {
        signatureVerifier.verify(timestamp, signature, rawBody);
        SandboxWebhookRequest event;
        try {
            event = objectMapper.readValue(rawBody, SandboxWebhookRequest.class);
        } catch (Exception exception) {
            throw new InvalidWebhookException("Webhook body is malformed.");
        }
        var violations = validator.validate(event);
        if (!violations.isEmpty()) {
            ConstraintViolation<SandboxWebhookRequest> violation = violations.iterator().next();
            throw new InvalidWebhookException("Webhook field " + violation.getPropertyPath() + " is invalid.");
        }
        return paymentService.processWebhook(event, signatureVerifier.payloadHash(rawBody));
    }
}
