package ru.mtuci.autonotesbackend.modules.notes.impl.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE_NOTES = "notes.exchange";
    public static final String QUEUE_NOTES_PROCESS = "notes.process.queue";
    public static final String QUEUE_NOTES_DLQ = "notes.process.dlq";
    public static final String ROUTING_KEY_PROCESS = "notes.created";
    public static final String ROUTING_KEY_DLQ = "notes.dlq";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public TopicExchange notesExchange() {
        return new TopicExchange(EXCHANGE_NOTES);
    }

    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(QUEUE_NOTES_DLQ).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlq()).to(notesExchange()).with(ROUTING_KEY_DLQ);
    }

    @Bean
    public Queue processingQueue() {
        return QueueBuilder.durable(QUEUE_NOTES_PROCESS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_NOTES)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Binding processingBinding() {
        return BindingBuilder.bind(processingQueue()).to(notesExchange()).with(ROUTING_KEY_PROCESS);
    }
}
