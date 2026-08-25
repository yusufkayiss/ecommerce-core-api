package com.ecommerce.api.controller;

import com.ecommerce.api.dto.AddToCartRequest;
import com.ecommerce.api.dto.CartResponse;
import com.ecommerce.api.entity.User;
import com.ecommerce.api.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @AuthenticationPrincipal User user,
            @RequestBody AddToCartRequest request) {
        CartResponse updatedCart = cartService.addToCart(user.getId(), request);
        return ResponseEntity.ok(updatedCart);
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal User user) {
        CartResponse cart = cartService.getCartByUserId(user.getId());
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeFromCart(
            @AuthenticationPrincipal User user,
            @PathVariable Long cartItemId) {
        CartResponse updatedCart = cartService.removeFromCart(user.getId(), cartItemId);
        return ResponseEntity.ok(updatedCart);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user.getId());
        return ResponseEntity.noContent().build();
    }
}