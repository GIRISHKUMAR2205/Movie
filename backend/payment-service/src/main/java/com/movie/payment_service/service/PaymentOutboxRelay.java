package com.movie.payment_service.service;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.movie.payment_service.config.PaymentOutboxProperties;
import com.movie.payment_service.entity.PaymentOutboxEvent;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "showhub.payment-events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentOutboxRelay {
    private final PaymentOutboxStore store;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentOutboxProperties properties;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${showhub.payment-events.relay-delay:PT1S}")
    public void relay() {
        for (PaymentOutboxEvent event : store.claim()) publish(event);
    }

    private void publish(PaymentOutboxEvent event) {
        try {
            CorrelationData correlation = new CorrelationData(event.getId().toString());
            rabbitTemplate.convertAndSend(properties.exchange(), properties.routingKey(), event.getPayload(), message -> {
                MessageProperties headers = message.getMessageProperties();
                headers.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                headers.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                headers.setMessageId(event.getId().toString());
                headers.setHeader("eventId", event.getId().toString());
                headers.setHeader("eventType", "PaymentStatusChanged");
                return message;
            }, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(properties.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (confirm == null || !confirm.isAck()) {
                throw new IllegalStateException(confirm == null ? "Publisher confirm timed out" : confirm.getReason());
            }
            store.published(event.getId());
            meterRegistry.counter("showhub.payment.outbox.published").increment();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            requeue(event, error);
        } catch (Exception error) {
            requeue(event, error);
        }
    }

    private void requeue(PaymentOutboxEvent event, Exception error) {
        store.requeue(event.getId(), clock.instant().plus(backoff(event.getAttemptCount())), error.getMessage());
        meterRegistry.counter("showhub.payment.outbox.failures").increment();
    }

    private Duration backoff(int attempt) {
        long multiplier = 1L << Math.min(Math.max(attempt - 1, 0), 20);
        Duration delay = properties.initialRetryDelay().multipliedBy(multiplier);
        return delay.compareTo(properties.maxRetryDelay()) > 0 ? properties.maxRetryDelay() : delay;
    }
}
