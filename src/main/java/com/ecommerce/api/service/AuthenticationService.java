package com.ecommerce.api.service;

import com.ecommerce.api.dto.*;

public interface AuthenticationService {
    AuthenticationResponse register(RegisterRequest request);
    AuthenticationResponse authenticate(AuthenticationRequest request);
    TokenRefreshResponse refreshToken(TokenRefreshRequest request);
    void logout(String refreshToken);
}