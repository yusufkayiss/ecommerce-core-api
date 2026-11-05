package com.ecommerce.api.service;

import com.ecommerce.api.dto.request.AuthenticationRequest;
import com.ecommerce.api.dto.request.RegisterRequest;
import com.ecommerce.api.dto.request.TokenRefreshRequest;
import com.ecommerce.api.dto.response.AuthenticationResponse;
import com.ecommerce.api.dto.response.TokenRefreshResponse;

public interface AuthenticationService {
    AuthenticationResponse register(RegisterRequest request);
    AuthenticationResponse authenticate(AuthenticationRequest request);
    TokenRefreshResponse refreshToken(TokenRefreshRequest request);
    void logout(String refreshToken);
}