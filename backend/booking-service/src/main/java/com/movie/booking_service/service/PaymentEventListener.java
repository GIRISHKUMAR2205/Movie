package com.movie.booking_service.service;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.movie.booking_service.error.BookingConflictException;
import com.movie.booking_service.error.BookingNotFoundException;
import com.movie.booking_service.event.PaymentStatusChangedEvent;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    private final ObjectMapper objectMapper;
    private final PaymentEventProcessor processor;

    @RabbitListener(queues = "${showhub.payment-events.queue}")
    public void receive(String payload) {
        try {
            PaymentStatusChangedEvent event = objectMapper.readValue(payload, PaymentStatusChangedEvent.class);
            processor.process(event, payload);
        } catch (BookingConflictException | BookingNotFoundException | IllegalArgumentException error) {
            throw new AmqpRejectAndDontRequeueException("Payment event cannot be applied.", error);
        } catch (AmqpRejectAndDontRequeueException error) {
            throw error;
        } catch (Exception error) {
            if (error instanceof RuntimeException runtime) throw runtime;
            throw new AmqpRejectAndDontRequeueException("Malformed payment event.", error);
        }
    }
}
