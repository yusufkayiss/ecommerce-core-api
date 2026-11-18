package com.ecommerce.api.producer;

import com.ecommerce.api.config.RabbitMQConfig;
import com.ecommerce.api.dto.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        publish(RabbitMQConfig.EXCHANGE_ORDER, RabbitMQConfig.ROUTING_KEY_ORDER_CREATED, event);
    }

    public void publish(String exchange, String routingKey, Object event) {
        log.info("Publishing event to RabbitMQ [exchange={}, routingKey={}]: {}", exchange, routingKey, event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}