package com.ecommerce.api.service;

import com.ecommerce.api.dto.AuthenticationRequest;
import com.ecommerce.api.dto.AuthenticationResponse;
import com.ecommerce.api.dto.RegisterRequest;

public interface AuthenticationService {
    AuthenticationResponse register(RegisterRequest request);
    AuthenticationResponse authenticate(AuthenticationRequest request);
}