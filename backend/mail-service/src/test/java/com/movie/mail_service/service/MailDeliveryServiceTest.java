package com.movie.mail_service.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.movie.mail_service.event.EmailVerificationMailEvent;
import com.movie.mail_service.repository.MailDeliveryRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class MailDeliveryServiceTest {

    @Mock
    private MailDeliveryRepository mailDeliveryRepository;

    @Mock
    private EmailSender emailSender;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private MailDeliveryService mailDeliveryService;

    @Test
    void deliversAndRecordsANewEvent() {
        EmailVerificationMailEvent event = event();
        when(mailDeliveryRepository.reserve(event.eventId())).thenReturn(1);

        mailDeliveryService.deliver(event);

        verify(emailSender).send(event);
        verify(mailDeliveryRepository).markDelivered(org.mockito.ArgumentMatchers.eq(event.eventId()),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresAnAlreadyReservedOrDeliveredEvent() {
        EmailVerificationMailEvent event = event();
        when(mailDeliveryRepository.reserve(event.eventId())).thenReturn(0);

        mailDeliveryService.deliver(event);

        verify(emailSender, never()).send(event);
        verify(mailDeliveryRepository, never()).markDelivered(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private EmailVerificationMailEvent event() {
        return new EmailVerificationMailEvent(UUID.randomUUID(), "user@example.com", "Subject", "Body");
    }
}
