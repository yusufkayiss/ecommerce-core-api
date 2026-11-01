package com.ecommerce.api.controller;

import com.ecommerce.api.dto.AddToCartRequest;
import com.ecommerce.api.dto.CartResponse;
import com.ecommerce.api.entity.User;
import com.ecommerce.api.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CartResponse> addToCart(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AddToCartRequest request) {
        CartResponse updatedCart = cartService.addToCart(user.getId(), request);
        return ResponseEntity.ok(updatedCart);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal User user) {
        CartResponse cart = cartService.getCartByUserId(user.getId());
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CartResponse> removeFromCart(
            @AuthenticationPrincipal User user,
            @PathVariable Long cartItemId) {
        CartResponse updatedCart = cartService.removeFromCart(user.getId(), cartItemId);
        return ResponseEntity.ok(updatedCart);
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user.getId());
        return ResponseEntity.noContent().build();
    }
}