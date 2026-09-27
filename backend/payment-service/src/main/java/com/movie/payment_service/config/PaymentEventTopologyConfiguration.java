package com.movie.payment_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentEventTopologyConfiguration {
    @Bean
    TopicExchange paymentEventExchange(PaymentOutboxProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    DirectExchange paymentEventDeadLetterExchange(PaymentOutboxProperties properties) {
        return new DirectExchange(properties.deadLetterExchange(), true, false);
    }

    @Bean
    Queue paymentBookingQueue(PaymentOutboxProperties properties) {
        return QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterQueue()).build();
    }

    @Bean
    Queue paymentBookingDeadLetterQueue(PaymentOutboxProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    Binding paymentBookingBinding(Queue paymentBookingQueue, TopicExchange paymentEventExchange,
            PaymentOutboxProperties properties) {
        return BindingBuilder.bind(paymentBookingQueue).to(paymentEventExchange).with(properties.routingKey());
    }

    @Bean
    Binding paymentBookingDeadLetterBinding(Queue paymentBookingDeadLetterQueue,
            DirectExchange paymentEventDeadLetterExchange, PaymentOutboxProperties properties) {
        return BindingBuilder.bind(paymentBookingDeadLetterQueue).to(paymentEventDeadLetterExchange)
                .with(properties.deadLetterQueue());
    }
}
