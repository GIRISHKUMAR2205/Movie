package com.movie.user_service.service;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.movie.user_service.entity.OutboxRelayProperties;

/**
 * Declares the durable mail route before the relay can publish. The same
 * topology is declared by mail-service so either service can safely start
 * first.
 */
@Configuration
class OutboxRabbitTopologyConfiguration {

    @Bean
    TopicExchange outboxMailExchange(OutboxRelayProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    DirectExchange outboxMailDeadLetterExchange(OutboxRelayProperties properties) {
        return new DirectExchange(properties.deadLetterExchange(), true, false);
    }

    @Bean
    Queue outboxMailQueue(OutboxRelayProperties properties) {
        return QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterQueue())
                .build();
    }

    @Bean
    Queue outboxMailDeadLetterQueue(OutboxRelayProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    Binding outboxMailBinding(Queue outboxMailQueue, TopicExchange outboxMailExchange,
            OutboxRelayProperties properties) {
        return BindingBuilder.bind(outboxMailQueue).to(outboxMailExchange).with(properties.routingKey());
    }

    @Bean
    Binding outboxMailDeadLetterBinding(Queue outboxMailDeadLetterQueue,
            DirectExchange outboxMailDeadLetterExchange, OutboxRelayProperties properties) {
        return BindingBuilder.bind(outboxMailDeadLetterQueue).to(outboxMailDeadLetterExchange)
                .with(properties.deadLetterQueue());
    }
}
