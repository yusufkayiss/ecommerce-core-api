package com.ecommerce.api.service;

import com.ecommerce.api.dto.response.OrderResponse;
import com.ecommerce.api.entity.OrderStatus;
import com.ecommerce.api.exception.IdempotencyConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyService = new IdempotencyService(stringRedisTemplate, objectMapper);
    }

    @Test
    void shouldExecuteActionAndCacheResponseOnFirstRequest() {
        when(valueOperations.setIfAbsent(eq("idempotency:orders:1:req-123"), eq("IN_PROGRESS"), any(Duration.class)))
                .thenReturn(true);

        OrderResponse expected = OrderResponse.builder()
                .id(42L)
                .userId(1L)
                .totalAmount(new BigDecimal("199.90"))
                .status(OrderStatus.PENDING)
                .items(List.of())
                .createdAt(LocalDateTime.now())
                .build();

        OrderResponse actual = idempotencyService.execute("orders", 1L, "req-123", OrderResponse.class, () -> expected);

        assertEquals(42L, actual.getId());
        verify(valueOperations).set(eq("idempotency:orders:1:req-123"), anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    void shouldReturnCachedResponseWithoutReExecutingOnDuplicateRequest() throws Exception {
        OrderResponse cached = OrderResponse.builder()
                .id(42L)
                .userId(1L)
                .totalAmount(new BigDecimal("199.90"))
                .status(OrderStatus.PENDING)
                .items(List.of())
                .build();

        String json = objectMapper.writeValueAsString(cached);
        when(valueOperations.setIfAbsent(eq("idempotency:orders:1:req-123"), eq("IN_PROGRESS"), any(Duration.class)))
                .thenReturn(false);
        when(valueOperations.get("idempotency:orders:1:req-123")).thenReturn(json);

        AtomicInteger counter = new AtomicInteger(0);
        OrderResponse actual = idempotencyService.execute("orders", 1L, "req-123", OrderResponse.class, () -> {
            counter.incrementAndGet();
            return new OrderResponse();
        });

        assertEquals(0, counter.get());
        assertEquals(42L, actual.getId());
    }

    @Test
    void shouldThrowConflictWhenRequestIsStillInProgress() {
        when(valueOperations.setIfAbsent(eq("idempotency:orders:1:req-123"), eq("IN_PROGRESS"), any(Duration.class)))
                .thenReturn(false);
        when(valueOperations.get("idempotency:orders:1:req-123")).thenReturn("IN_PROGRESS");

        assertThrows(IdempotencyConflictException.class, () ->
                idempotencyService.execute("orders", 1L, "req-123", OrderResponse.class, OrderResponse::new)
        );
    }

    @Test
    void shouldReleaseLockWhenActionThrowsException() {
        when(valueOperations.setIfAbsent(eq("idempotency:orders:1:req-err"), eq("IN_PROGRESS"), any(Duration.class)))
                .thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                idempotencyService.execute("orders", 1L, "req-err", OrderResponse.class, () -> {
                    throw new RuntimeException("Stok yetersiz");
                })
        );

        verify(stringRedisTemplate).delete("idempotency:orders:1:req-err");
    }
}