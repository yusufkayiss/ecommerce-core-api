package com.ecommerce.api.service;

import com.ecommerce.api.dto.AddToCartRequest;
import com.ecommerce.api.dto.CartResponse;

public interface CartService {
    CartResponse getCartByUserId(Long userId);
    CartResponse addToCart(Long userId, AddToCartRequest request);
    CartResponse removeFromCart(Long userId, Long cartItemId);
    void clearCart(Long userId);
}