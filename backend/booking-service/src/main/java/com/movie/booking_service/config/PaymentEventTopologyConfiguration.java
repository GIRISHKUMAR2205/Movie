package com.movie.booking_service.config;

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
    TopicExchange paymentEventExchange(PaymentEventProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    DirectExchange paymentEventDeadLetterExchange(PaymentEventProperties properties) {
        return new DirectExchange(properties.deadLetterExchange(), true, false);
    }

    @Bean
    Queue paymentEventQueue(PaymentEventProperties properties) {
        return QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterQueue()).build();
    }

    @Bean
    Queue paymentEventDeadLetterQueue(PaymentEventProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    Binding paymentEventBinding(Queue paymentEventQueue, TopicExchange paymentEventExchange,
            PaymentEventProperties properties) {
        return BindingBuilder.bind(paymentEventQueue).to(paymentEventExchange).with(properties.routingKey());
    }

    @Bean
    Binding paymentEventDeadLetterBinding(Queue paymentEventDeadLetterQueue,
            DirectExchange paymentEventDeadLetterExchange, PaymentEventProperties properties) {
        return BindingBuilder.bind(paymentEventDeadLetterQueue).to(paymentEventDeadLetterExchange)
                .with(properties.deadLetterQueue());
    }
}
