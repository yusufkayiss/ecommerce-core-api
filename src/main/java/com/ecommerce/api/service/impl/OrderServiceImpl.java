package com.ecommerce.api.service.impl;

import com.ecommerce.api.config.RabbitMQConfig;
import com.ecommerce.api.dto.event.OrderCreatedEvent;
import com.ecommerce.api.dto.response.OrderItemResponse;
import com.ecommerce.api.dto.response.OrderResponse;
import com.ecommerce.api.entity.*;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.CartRepository;
import com.ecommerce.api.repository.OrderRepository;
import com.ecommerce.api.repository.OutboxEventRepository;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.service.IdempotencyService;
import com.ecommerce.api.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final IdempotencyService idempotencyService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final Tracer tracer;
    private final Propagator propagator;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId) {
        return executeCreateOrder(userId);
    }

    @Override
    public OrderResponse createOrder(Long userId, String idempotencyKey) {
        return idempotencyService.execute(
                "orders",
                userId,
                idempotencyKey,
                OrderResponse.class,
                () -> transactionTemplate.execute(status -> executeCreateOrder(userId))
        );
    }

    private OrderResponse executeCreateOrder(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sepet bulunamadÄ±"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("BoÅŸ sepet ile sipariÅŸ oluÅŸturulamaz");
        }

        Order order = Order.builder()
                .user(cart.getUser())
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Stok yetersiz: " + product.getName());
            }

            // Stok DÃ¼ÅŸÃ¼rme
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal itemTotalPrice = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(itemTotalPrice);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // Sepeti Temizleme
        cart.getItems().clear();
        cartRepository.save(cart);

        // Transactional Outbox: SipariÅŸ ile aynÄ± DB transaction'Ä±nda Outbox tablosuna kayÄ±t
        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUser().getEmail(),
                savedOrder.getTotalAmount(),
                LocalDateTime.now()
        );

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("ORDER")
                .aggregateId(String.valueOf(savedOrder.getId()))
                .eventType("OrderCreatedEvent")
                .exchangeName(RabbitMQConfig.EXCHANGE_ORDER)
                .routingKey(RabbitMQConfig.ROUTING_KEY_ORDER_CREATED)
                .payload(serializeEvent(orderCreatedEvent))
                .traceContext(captureTraceContext())
                .status(OutboxStatus.PENDING)
                .build();

        outboxEventRepository.save(outboxEvent);

        evictProductCaches();

        return mapToResponse(savedOrder);
    }

    private void evictProductCaches() {
        List.of("product", "products", "products_by_category").forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    private String captureTraceContext() {
        try {
            if (tracer == null || propagator == null || tracer.currentSpan() == null) {
                return null;
            }
            Map<String, String> carrier = new LinkedHashMap<>();
            propagator.inject(tracer.currentSpan().context(), carrier, Map::put);
            return carrier.isEmpty() ? null : objectMapper.writeValueAsString(carrier);
        } catch (Exception e) {
            return null;
        }
    }

    private String serializeEvent(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox event JSON formatÄ±na dÃ¶nÃ¼ÅŸtÃ¼rÃ¼lemedi", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("SipariÅŸ bulunamadÄ±: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bu sipariÅŸi gÃ¶rÃ¼ntÃ¼leme yetkiniz yok");
        }

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("SipariÅŸ bulunamadÄ±: " + orderId));

        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        return mapToResponse(updatedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
