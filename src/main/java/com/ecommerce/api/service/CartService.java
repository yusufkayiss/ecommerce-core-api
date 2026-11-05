package com.ecommerce.api.service;

import com.ecommerce.api.dto.request.AddToCartRequest;
import com.ecommerce.api.dto.response.CartResponse;

public interface CartService {

    CartResponse getCartByUserId(Long userId);

    CartResponse addToCart(Long userId, AddToCartRequest request);

    CartResponse updateItemQuantity(Long userId, Long cartItemId, Integer quantity);

    CartResponse removeItemFromCart(Long userId, Long cartItemId);

    void clearCart(Long userId);
}