package com.movie.mail_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfiguration {

    @Bean
    TopicExchange mailExchange(MailProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    DirectExchange mailDeadLetterExchange(MailProperties properties) {
        return new DirectExchange(properties.deadLetterExchange(), true, false);
    }

    @Bean
    Queue mailQueue(MailProperties properties) {
        return QueueBuilder.durable(properties.queue())
                .deadLetterExchange(properties.deadLetterExchange())
                .deadLetterRoutingKey(properties.deadLetterQueue())
                .build();
    }

    @Bean
    Queue mailDeadLetterQueue(MailProperties properties) {
        return QueueBuilder.durable(properties.deadLetterQueue()).build();
    }

    @Bean
    Binding mailBinding(Queue mailQueue, TopicExchange mailExchange, MailProperties properties) {
        return BindingBuilder.bind(mailQueue).to(mailExchange).with(properties.routingKey());
    }

    @Bean
    Binding mailDeadLetterBinding(Queue mailDeadLetterQueue, DirectExchange mailDeadLetterExchange,
            MailProperties properties) {
        return BindingBuilder.bind(mailDeadLetterQueue).to(mailDeadLetterExchange)
                .with(properties.deadLetterQueue());
    }
}
