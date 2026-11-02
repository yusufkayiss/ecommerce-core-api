package com.ecommerce.api.controller;

import com.ecommerce.api.dto.*;
import com.ecommerce.api.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        return ResponseEntity.ok(service.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        service.logout(request.getRefreshToken());
        return ResponseEntity.ok("Başarıyla çıkış yapıldı.");
    }
}