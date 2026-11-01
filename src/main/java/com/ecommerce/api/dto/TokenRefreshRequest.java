package com.ecommerce.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh token boş olamaz")
    private String refreshToken;
}