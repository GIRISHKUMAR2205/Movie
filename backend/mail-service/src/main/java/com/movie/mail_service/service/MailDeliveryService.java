package com.movie.mail_service.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.movie.mail_service.event.EmailVerificationMailEvent;
import com.movie.mail_service.repository.MailDeliveryRepository;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailDeliveryService {

    private final MailDeliveryRepository mailDeliveryRepository;
    private final EmailSender emailSender;
    private final MeterRegistry meterRegistry;

    /**
     * An INSERT ... ON CONFLICT reservation makes duplicate broker deliveries a
     * no-op. The delivery row is committed only after SMTP accepts the message;
     * an exception rolls it back and lets RabbitMQ retry the event.
     */
    @Transactional
    public void deliver(EmailVerificationMailEvent event) {
        if (mailDeliveryRepository.reserve(event.eventId()) == 0) {
            meterRegistry.counter("showhub.mail.delivery.duplicates").increment();
            return;
        }

        emailSender.send(event);
        mailDeliveryRepository.markDelivered(event.eventId(), Instant.now());
        meterRegistry.counter("showhub.mail.delivery.sent").increment();
    }
}
