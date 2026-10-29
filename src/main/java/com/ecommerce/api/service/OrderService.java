package com.ecommerce.api.service;

import com.ecommerce.api.dto.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Long userId);
    OrderResponse getOrderById(Long orderId, Long userId);
    List<OrderResponse> getUserOrders(Long userId);
}