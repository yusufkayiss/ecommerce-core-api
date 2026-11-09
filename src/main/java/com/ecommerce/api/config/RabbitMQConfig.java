package com.ecommerce.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_ORDER_CREATED = "order.created.queue";
    public static final String EXCHANGE_ORDER = "order.exchange";
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created.routingKey";

    // DLQ ve DLX Sabitleri
    public static final String QUEUE_ORDER_DLQ = "order.created.dlq";
    public static final String EXCHANGE_ORDER_DLX = "order.dlx";
    public static final String ROUTING_KEY_ORDER_DLQ = "order.created.dlq.routingKey";

    // Ana Kuyruk (İşlenemeyen mesajlar varsayılan olarak DLX'e yönlendirilir)
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CREATED)
                .withArgument("x-dead-letter-exchange", EXCHANGE_ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_ORDER_DLQ)
                .build();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    @Bean
    public Binding binding(Queue orderCreatedQueue, TopicExchange orderExchange) {
        return BindingBuilder
                .bind(orderCreatedQueue)
                .to(orderExchange)
                .with(ROUTING_KEY_ORDER_CREATED);
    }

    // --- Dead Letter Queue (DLQ) Bean Tanımları ---
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_DLQ).build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(EXCHANGE_ORDER_DLX);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(ROUTING_KEY_ORDER_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}