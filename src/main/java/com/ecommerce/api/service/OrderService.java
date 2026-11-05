package com.ecommerce.api.service;

import com.ecommerce.api.dto.response.OrderResponse;
import com.ecommerce.api.entity.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(Long userId);

    OrderResponse getOrderById(Long userId, Long orderId);

    List<OrderResponse> getUserOrders(Long userId);

    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);
}