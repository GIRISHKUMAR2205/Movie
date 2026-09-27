package com.movie.payment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.movie.payment_service.dto.CreatePaymentRequest;
import com.movie.payment_service.entity.Payment;
import com.movie.payment_service.entity.PaymentMethod;
import com.movie.payment_service.error.PaymentConflictException;
import com.movie.payment_service.provider.PaymentProvider;
import com.movie.payment_service.provider.ProviderPayment;
import com.movie.payment_service.client.BookingClient;
import com.movie.payment_service.client.BookingPaymentQuote;
import com.movie.payment_service.repository.PaymentAuditRepository;
import com.movie.payment_service.repository.PaymentRepository;
import com.movie.payment_service.repository.ProcessedWebhookEventRepository;
import com.movie.payment_service.repository.RefundRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentAuditRepository auditRepository;
    @Mock RefundRepository refundRepository;
    @Mock ProcessedWebhookEventRepository webhookEventRepository;
    @Mock PaymentProvider paymentProvider;
    @Mock BookingClient bookingClient;
    @Mock PaymentEventOutboxPublisher eventPublisher;

    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentApplicationService(paymentRepository, auditRepository, refundRepository,
                webhookEventRepository, paymentProvider, bookingClient, eventPublisher,
                Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC), new SimpleMeterRegistry());
    }

    @Test
    void returnsTheOriginalPaymentWhenAnIdenticalIdempotencyKeyIsRetried() {
        String owner = "user@example.com";
        String key = "checkout-attempt-1";
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), PaymentMethod.UPI);
        AtomicReference<Payment> saved = new AtomicReference<>();
        when(paymentRepository.findByOwnerSubjectAndIdempotencyKey(owner, key))
                .thenAnswer(invocation -> Optional.ofNullable(saved.get()));
        when(paymentRepository.existsByBookingIdAndStatusIn(any(), any())).thenReturn(false);
        when(bookingClient.startPayment(request.bookingId(), owner)).thenReturn(new BookingPaymentQuote(
                request.bookingId(), owner, new BigDecimal("499.00"), "INR", "PAYMENT_PROCESSING",
                Instant.parse("2026-09-23T12:30:00Z")));
        when(paymentProvider.create(any(), any(), any(), any()))
                .thenAnswer(invocation -> new ProviderPayment("sandbox_pay_1", "http://checkout/1"));
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            saved.set(payment);
            return payment;
        });

        var first = service.create(owner, key, request);
        var retry = service.create(owner, key, request);

        assertThat(retry.id()).isEqualTo(first.id());
        verify(paymentProvider).create(any(), any(), any(), any());
    }

    @Test
    void rejectsReuseOfAnIdempotencyKeyForDifferentPaymentData() {
        String owner = "user@example.com";
        Payment existing = new Payment();
        existing.setId(UUID.randomUUID());
        existing.setBookingId(UUID.randomUUID());
        existing.setAmount(new BigDecimal("499.00"));
        existing.setCurrency("INR");
        existing.setPaymentMethod(PaymentMethod.CARD);
        when(paymentRepository.findByOwnerSubjectAndIdempotencyKey(owner, "same-key"))
                .thenReturn(Optional.of(existing));

        CreatePaymentRequest changed = new CreatePaymentRequest(existing.getBookingId(), PaymentMethod.UPI);

        assertThatThrownBy(() -> service.create(owner, "same-key", changed))
                .isInstanceOf(PaymentConflictException.class);
        verify(paymentProvider, never()).create(any(), any(), any(), any());
    }
}
