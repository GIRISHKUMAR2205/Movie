package com.movie.payment_service.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movie.payment_service.dto.CreatePaymentRequest;
import com.movie.payment_service.dto.PaymentResponse;
import com.movie.payment_service.dto.RefundRequest;
import com.movie.payment_service.dto.RefundResponse;
import com.movie.payment_service.dto.SandboxWebhookRequest;
import com.movie.payment_service.client.BookingClient;
import com.movie.payment_service.client.BookingPaymentQuote;
import com.movie.payment_service.entity.Payment;
import com.movie.payment_service.entity.PaymentAudit;
import com.movie.payment_service.entity.PaymentStatus;
import com.movie.payment_service.entity.ProcessedWebhookEvent;
import com.movie.payment_service.entity.Refund;
import com.movie.payment_service.entity.RefundStatus;
import com.movie.payment_service.error.InvalidWebhookException;
import com.movie.payment_service.error.PaymentConflictException;
import com.movie.payment_service.error.PaymentNotFoundException;
import com.movie.payment_service.provider.PaymentProvider;
import com.movie.payment_service.provider.ProviderPayment;
import com.movie.payment_service.repository.PaymentAuditRepository;
import com.movie.payment_service.repository.PaymentRepository;
import com.movie.payment_service.repository.ProcessedWebhookEventRepository;
import com.movie.payment_service.repository.RefundRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class PaymentApplicationService {
    private static final EnumSet<PaymentStatus> ACTIVE_BOOKING_PAYMENTS = EnumSet.of(
            PaymentStatus.PENDING, PaymentStatus.SUCCEEDED,
            PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED);

    private final PaymentRepository paymentRepository;
    private final PaymentAuditRepository auditRepository;
    private final RefundRepository refundRepository;
    private final ProcessedWebhookEventRepository webhookEventRepository;
    private final PaymentProvider paymentProvider;
    private final BookingClient bookingClient;
    private final PaymentEventOutboxPublisher eventPublisher;
    private final Clock clock;
    private final Counter startedCounter;
    private final Counter succeededCounter;
    private final Counter failedCounter;
    private final Counter refundedCounter;

    public PaymentApplicationService(PaymentRepository paymentRepository,
            PaymentAuditRepository auditRepository,
            RefundRepository refundRepository,
            ProcessedWebhookEventRepository webhookEventRepository,
            PaymentProvider paymentProvider,
            BookingClient bookingClient,
            PaymentEventOutboxPublisher eventPublisher,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.auditRepository = auditRepository;
        this.refundRepository = refundRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.paymentProvider = paymentProvider;
        this.bookingClient = bookingClient;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.startedCounter = meterRegistry.counter("showhub.payment.started");
        this.succeededCounter = meterRegistry.counter("showhub.payment.succeeded");
        this.failedCounter = meterRegistry.counter("showhub.payment.failed");
        this.refundedCounter = meterRegistry.counter("showhub.payment.refunded");
    }

    @Transactional
    public PaymentResponse create(String ownerSubject, String idempotencyKey, CreatePaymentRequest request) {
        String key = validIdempotencyKey(idempotencyKey);
        var existing = paymentRepository.findByOwnerSubjectAndIdempotencyKey(ownerSubject, key);
        if (existing.isPresent()) {
            ensureSameRequest(existing.get(), request);
            return toResponse(existing.get());
        }
        if (paymentRepository.existsByBookingIdAndStatusIn(request.bookingId(), ACTIVE_BOOKING_PAYMENTS)) {
            throw new PaymentConflictException("An active payment already exists for this booking.");
        }
        BookingPaymentQuote quote = bookingClient.startPayment(request.bookingId(), ownerSubject);
        if (!quote.bookingId().equals(request.bookingId()) || !quote.ownerSubject().equals(ownerSubject)) {
            throw new PaymentConflictException("Booking quote did not match the payment request.");
        }

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(request.bookingId());
        payment.setOwnerSubject(ownerSubject);
        payment.setAmount(money(quote.amount()));
        payment.setCurrency(quote.currency().toUpperCase(Locale.ROOT));
        payment.setPaymentMethod(request.paymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(key);
        ProviderPayment providerPayment = paymentProvider.create(
                payment.getId(), payment.getAmount(), payment.getCurrency(), payment.getPaymentMethod());
        payment.setProviderReference(providerPayment.providerReference());
        payment.setCheckoutUrl(providerPayment.checkoutUrl());
        paymentRepository.save(payment);
        audit(payment, null, PaymentStatus.PENDING, ownerSubject, "Payment initiated.");
        eventPublisher.publish(payment);
        startedCounter.increment();
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(String ownerSubject, UUID paymentId) {
        return toResponse(ownedPayment(ownerSubject, paymentId));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list(String ownerSubject) {
        return paymentRepository.findAllByOwnerSubjectOrderByCreatedAtDesc(ownerSubject).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public PaymentResponse cancel(String ownerSubject, UUID paymentId) {
        Payment payment = ownedPayment(ownerSubject, paymentId);
        requireStatus(payment, PaymentStatus.PENDING);
        transition(payment, PaymentStatus.CANCELLED, ownerSubject, "Payment cancelled by its owner.");
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse processWebhook(SandboxWebhookRequest request, String payloadHash) {
        ProcessedWebhookEvent processed = webhookEventRepository.findById(request.eventId()).orElse(null);
        if (processed != null) {
            if (!processed.getPayloadHash().equals(payloadHash)) {
                throw new InvalidWebhookException("A webhook event ID was reused with different content.");
            }
            Payment duplicatePayment = paymentRepository.findByProviderReference(request.providerReference())
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found."));
            return toResponse(duplicatePayment);
        }
        if (request.status() != PaymentStatus.SUCCEEDED && request.status() != PaymentStatus.FAILED) {
            throw new IllegalArgumentException("A provider webhook may only report SUCCEEDED or FAILED.");
        }
        Payment payment = paymentRepository.findByProviderReference(request.providerReference())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found."));
        if (payment.getStatus() == request.status()) {
            rememberWebhook(request.eventId(), payloadHash);
            return toResponse(payment);
        }
        requireStatus(payment, PaymentStatus.PENDING);
        if (request.status() == PaymentStatus.FAILED) {
            payment.setFailureCode(blankToNull(request.failureCode()));
            payment.setFailureMessage(blankToNull(request.failureMessage()));
            failedCounter.increment();
        } else {
            payment.setFailureCode(null);
            payment.setFailureMessage(null);
            succeededCounter.increment();
        }
        transition(payment, request.status(), "provider:sandbox", "Signed provider webhook " + request.eventId());
        rememberWebhook(request.eventId(), payloadHash);
        return toResponse(payment);
    }

    private void rememberWebhook(String eventId, String payloadHash) {
        ProcessedWebhookEvent event = new ProcessedWebhookEvent();
        event.setEventId(eventId);
        event.setPayloadHash(payloadHash);
        event.setProcessedAt(clock.instant());
        webhookEventRepository.save(event);
    }

    @Transactional
    public RefundResponse refund(String administrator, UUID paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found."));
        if (payment.getStatus() != PaymentStatus.SUCCEEDED
                && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new PaymentConflictException("Only a successful payment can be refunded.");
        }
        BigDecimal remaining = payment.getAmount().subtract(payment.getRefundedAmount());
        BigDecimal amount = request.amount() == null ? remaining : money(request.amount());
        if (amount.compareTo(remaining) > 0) {
            throw new PaymentConflictException("Refund amount exceeds the remaining captured amount.");
        }
        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setPayment(payment);
        refund.setAmount(amount);
        refund.setReason(request.reason().trim());
        refund.setRequestedBy(administrator);
        refund.setStatus(RefundStatus.SUCCEEDED);
        refund.setProviderReference(paymentProvider.refund(
                refund.getId(), payment.getProviderReference(), amount, payment.getCurrency()));
        refundRepository.save(refund);

        PaymentStatus previous = payment.getStatus();
        payment.setRefundedAmount(payment.getRefundedAmount().add(amount));
        PaymentStatus current = payment.getRefundedAmount().compareTo(payment.getAmount()) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        payment.setStatus(current);
        audit(payment, previous, current, administrator, request.reason().trim());
        eventPublisher.publish(payment);
        refundedCounter.increment();
        return toRefundResponse(refund);
    }

    private Payment ownedPayment(String ownerSubject, UUID paymentId) {
        return paymentRepository.findByIdAndOwnerSubject(paymentId, ownerSubject)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found."));
    }

    private void transition(Payment payment, PaymentStatus current, String actor, String reason) {
        PaymentStatus previous = payment.getStatus();
        payment.setStatus(current);
        audit(payment, previous, current, actor, reason);
        eventPublisher.publish(payment);
    }

    private void audit(Payment payment, PaymentStatus previous, PaymentStatus current, String actor, String reason) {
        PaymentAudit audit = new PaymentAudit();
        audit.setPayment(payment);
        audit.setPreviousStatus(previous);
        audit.setCurrentStatus(current);
        audit.setActor(actor);
        audit.setReason(reason);
        auditRepository.save(audit);
    }

    private void requireStatus(Payment payment, PaymentStatus expected) {
        if (payment.getStatus() != expected) {
            throw new PaymentConflictException(
                    "Payment is " + payment.getStatus() + "; expected " + expected + ".");
        }
    }

    private void ensureSameRequest(Payment payment, CreatePaymentRequest request) {
        if (!payment.getBookingId().equals(request.bookingId())
                || payment.getPaymentMethod() != request.paymentMethod()) {
            throw new PaymentConflictException("The idempotency key was already used for a different payment request.");
        }
    }

    private String validIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key is required and must not exceed 128 characters.");
        }
        return value.trim();
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount.scale() > 2) throw new IllegalArgumentException("Amounts must have at most two decimal places.");
        return amount.setScale(2);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getBookingId(), payment.getAmount(),
                payment.getRefundedAmount(), payment.getCurrency(), payment.getPaymentMethod(), payment.getStatus(),
                payment.getProviderReference(), payment.getCheckoutUrl(), payment.getFailureCode(),
                payment.getFailureMessage(), payment.getCreatedAt(), payment.getUpdatedAt());
    }

    private RefundResponse toRefundResponse(Refund refund) {
        return new RefundResponse(refund.getId(), refund.getPayment().getId(), refund.getAmount(), refund.getStatus(),
                refund.getProviderReference(), refund.getReason(), refund.getCreatedAt());
    }
}
