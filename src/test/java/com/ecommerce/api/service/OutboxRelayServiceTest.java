package com.ecommerce.api.service;

import com.ecommerce.api.config.RabbitMQConfig;
import com.ecommerce.api.dto.event.OrderCreatedEvent;
import com.ecommerce.api.entity.OutboxEvent;
import com.ecommerce.api.entity.OutboxStatus;
import com.ecommerce.api.producer.OrderEventProducer;
import com.ecommerce.api.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    private ObjectMapper objectMapper;
    private OutboxRelayService outboxRelayService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        outboxRelayService = new OutboxRelayService(outboxEventRepository, orderEventProducer, objectMapper);
    }

    @Test
    void shouldPublishPendingEventAndMarkAsPublished() throws Exception {
        OrderCreatedEvent payloadObj = new OrderCreatedEvent(
                55L, "user@example.com", new BigDecimal("499.90"), LocalDateTime.now()
        );

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(1L)
                .aggregateType("ORDER")
                .aggregateId("55")
                .eventType("OrderCreatedEvent")
                .exchangeName(RabbitMQConfig.EXCHANGE_ORDER)
                .routingKey(RabbitMQConfig.ROUTING_KEY_ORDER_CREATED)
                .payload(objectMapper.writeValueAsString(payloadObj))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(outboxEvent));

        outboxRelayService.processPendingEvents();

        verify(orderEventProducer).publish(
                eq(RabbitMQConfig.EXCHANGE_ORDER),
                eq(RabbitMQConfig.ROUTING_KEY_ORDER_CREATED),
                any(OrderCreatedEvent.class)
        );
        assertEquals(OutboxStatus.PUBLISHED, outboxEvent.getStatus());
        assertNotNull(outboxEvent.getPublishedAt());
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    void shouldIncrementRetryCountWhenBrokerFails() throws Exception {
        OrderCreatedEvent payloadObj = new OrderCreatedEvent(
                56L, "user@example.com", new BigDecimal("120.00"), LocalDateTime.now()
        );

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(2L)
                .aggregateType("ORDER")
                .aggregateId("56")
                .eventType("OrderCreatedEvent")
                .exchangeName(RabbitMQConfig.EXCHANGE_ORDER)
                .routingKey(RabbitMQConfig.ROUTING_KEY_ORDER_CREATED)
                .payload(objectMapper.writeValueAsString(payloadObj))
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(outboxEvent));
        doThrow(new RuntimeException("RabbitMQ connection refused"))
                .when(orderEventProducer).publish(any(), any(), any());

        outboxRelayService.processPendingEvents();

        assertEquals(OutboxStatus.PENDING, outboxEvent.getStatus());
        assertEquals(1, outboxEvent.getRetryCount());
        assertTrue(outboxEvent.getLastError().contains("RabbitMQ connection refused"));
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    void shouldMarkAsFailedAfterMaxRetriesReached() throws Exception {
        OrderCreatedEvent payloadObj = new OrderCreatedEvent(
                57L, "user@example.com", new BigDecimal("99.00"), LocalDateTime.now()
        );

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(3L)
                .aggregateType("ORDER")
                .aggregateId("57")
                .eventType("OrderCreatedEvent")
                .exchangeName(RabbitMQConfig.EXCHANGE_ORDER)
                .routingKey(RabbitMQConfig.ROUTING_KEY_ORDER_CREATED)
                .payload(objectMapper.writeValueAsString(payloadObj))
                .status(OutboxStatus.PENDING)
                .retryCount(4)
                .createdAt(LocalDateTime.now())
                .build();

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(outboxEvent));
        doThrow(new RuntimeException("Broker down"))
                .when(orderEventProducer).publish(any(), any(), any());

        outboxRelayService.processPendingEvents();

        assertEquals(OutboxStatus.FAILED, outboxEvent.getStatus());
        assertEquals(5, outboxEvent.getRetryCount());
        verify(outboxEventRepository).save(outboxEvent);
    }
}