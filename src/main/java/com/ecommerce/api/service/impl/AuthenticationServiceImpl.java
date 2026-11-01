package com.ecommerce.api.service.impl;

import com.ecommerce.api.config.JwtService;
import com.ecommerce.api.dto.*;
import com.ecommerce.api.entity.RefreshToken;
import com.ecommerce.api.entity.Role;
import com.ecommerce.api.entity.User;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.service.AuthenticationService;
import com.ecommerce.api.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        repository.save(user);

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Override
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        return refreshTokenService.findByToken(request.getRefreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateToken(user);
                    return TokenRefreshResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(request.getRefreshToken())
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh token veritabanında bulunamadı!"));
    }
}