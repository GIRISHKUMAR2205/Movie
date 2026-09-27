package com.movie.mail_service.service;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.movie.mail_service.event.EmailVerificationMailEvent;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class MailEventListener {

    private final ObjectMapper objectMapper;
    private final MailDeliveryService mailDeliveryService;
    private final MeterRegistry meterRegistry;

    @RabbitListener(queues = "${showhub.mail.queue}")
    public void receive(String payload) {
        final EmailVerificationMailEvent event;
        try {
            event = objectMapper.readValue(payload, EmailVerificationMailEvent.class);
        } catch (Exception exception) {
            // Malformed messages cannot succeed on a retry; route them to the DLQ.
            throw new AmqpRejectAndDontRequeueException("Invalid mail event", exception);
        }
        try {
            mailDeliveryService.deliver(event);
        } catch (RuntimeException exception) {
            meterRegistry.counter("showhub.mail.delivery.failures").increment();
            throw exception;
        }
    }
}
