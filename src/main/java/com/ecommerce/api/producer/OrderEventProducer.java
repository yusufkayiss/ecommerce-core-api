package com.ecommerce.api.producer;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
@Slf4j
public class OrderEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final Tracer tracer;
    private final Propagator propagator;

    public OrderEventProducer(RabbitTemplate rabbitTemplate) {
        this(rabbitTemplate, null, null);
    }

    @Autowired
    public OrderEventProducer(RabbitTemplate rabbitTemplate, Tracer tracer, Propagator propagator) {
        this.rabbitTemplate = rabbitTemplate;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public void publish(String exchangeName, String routingKey, Object eventPayload) {
        publish(exchangeName, routingKey, eventPayload, Collections.emptyMap());
    }

    public void publish(String exchangeName, String routingKey, Object eventPayload, Map<String, String> traceHeaders) {
        MessagePostProcessor headerPostProcessor = message -> {
            if (traceHeaders != null) {
                traceHeaders.forEach((key, value) -> {
                    if (key != null && value != null) {
                        message.getMessageProperties().setHeader(key, value);
                    }
                });
            }
            if (tracer != null && propagator != null) {
                Span currentSpan = tracer.currentSpan();
                if (currentSpan != null) {
                    propagator.inject(currentSpan.context(), message.getMessageProperties(),
                            (props, key, value) -> props.setHeader(key, value));
                }
            }
            return message;
        };

        rabbitTemplate.convertAndSend(exchangeName, routingKey, eventPayload, headerPostProcessor);
    }
}