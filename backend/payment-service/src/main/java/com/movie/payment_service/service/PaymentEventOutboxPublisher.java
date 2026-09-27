package com.movie.payment_service.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.movie.payment_service.entity.OutboxEventStatus;
import com.movie.payment_service.entity.Payment;
import com.movie.payment_service.entity.PaymentOutboxEvent;
import com.movie.payment_service.event.PaymentStatusChangedEvent;
import com.movie.payment_service.repository.PaymentOutboxEventRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class PaymentEventOutboxPublisher {
    private final PaymentOutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public void publish(Payment payment) {
        UUID eventId = UUID.randomUUID();
        PaymentStatusChangedEvent event = new PaymentStatusChangedEvent(eventId, payment.getId(),
                payment.getBookingId(), payment.getOwnerSubject(), payment.getAmount(), payment.getCurrency(),
                payment.getStatus(), clock.instant());
        PaymentOutboxEvent outbox = new PaymentOutboxEvent();
        outbox.setId(eventId);
        outbox.setPaymentId(payment.getId());
        try {
            outbox.setPayload(objectMapper.writeValueAsString(event));
        } catch (Exception error) {
            throw new IllegalStateException("Unable to serialize payment event.", error);
        }
        outbox.setStatus(OutboxEventStatus.PENDING);
        outbox.setCreatedAt(clock.instant());
        outbox.setNextAttemptAt(clock.instant());
        repository.save(outbox);
    }
}
