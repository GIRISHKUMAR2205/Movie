package com.movie.user_service.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.movie.user_service.entity.OutboxEvent;
import com.movie.user_service.entity.OutboxRelayProperties;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

/**
 * Relays persisted outbox commands to RabbitMQ. A broker confirmation is
 * required before an event is marked published. A process crash after a broker
 * acknowledgement can replay a message; mail-service handles that safely via
 * its event-id reservation.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "showhub.outbox.relay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqOutboxRelay {

    private final OutboxRelayStore store;
    private final RabbitTemplate rabbitTemplate;
    private final OutboxRelayProperties properties;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${showhub.outbox.relay.fixed-delay:PT1S}")
    public void relayPendingEvents() {
        List<OutboxEvent> events = store.claimNextBatch();
        for (OutboxEvent event : events) {
            relay(event);
        }
    }

    private void relay(OutboxEvent event) {
        try {
            publishWithConfirm(event);
            store.markPublished(event.getId());
            meterRegistry.counter("showhub.mail.outbox.events.published", "event_type", event.getType()).increment();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            requeue(event, exception);
        } catch (Exception exception) {
            requeue(event, exception);
        }
    }

    private void publishWithConfirm(OutboxEvent event) throws Exception {
        CorrelationData correlation = new CorrelationData(event.getId().toString());
        rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), event.getPayload(), message -> {
            MessageProperties messageProperties = message.getMessageProperties();
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            messageProperties.setMessageId(event.getId().toString());
            messageProperties.setHeader("eventId", event.getId().toString());
            messageProperties.setHeader("eventType", event.getType());
            return message;
        }, correlation);

        CorrelationData.Confirm confirm = correlation.getFuture()
                .get(properties.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
        if (confirm == null || !confirm.isAck()) {
            String reason = confirm == null ? "RabbitMQ publisher confirm timed out" : confirm.reason();
            throw new IllegalStateException(reason == null ? "RabbitMQ rejected publication" : reason);
        }
    }

    private void requeue(OutboxEvent event, Exception exception) {
        store.requeue(event.getId(), Instant.now().plus(backoff(event.getAttemptCount())), exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage());
        meterRegistry.counter("showhub.mail.outbox.events.publish.failures", "event_type", event.getType()).increment();
    }

    private Duration backoff(int attempt) {
        long multiplier = 1L << Math.min(Math.max(attempt - 1, 0), 20);
        Duration delay = properties.initialRetryDelay().multipliedBy(multiplier);
        return delay.compareTo(properties.maxRetryDelay()) > 0 ? properties.maxRetryDelay() : delay;
    }
}
