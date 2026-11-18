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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
                .orElseThrow(() -> new ResourceNotFoundException("Sepet bulunamadı"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Boş sepet ile sipariş oluşturulamaz");
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

            // Stok Düşürme
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

        // Transactional Outbox: Sipariş ile aynı DB transaction'ında Outbox tablosuna kayıt
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
                .status(OutboxStatus.PENDING)
                .build();

        outboxEventRepository.save(outboxEvent);

        return mapToResponse(savedOrder);
    }

    private String serializeEvent(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Outbox event JSON formatına dönüştürülemedi", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bu siparişi görüntüleme yetkiniz yok");
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
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı: " + orderId));

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