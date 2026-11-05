package com.ecommerce.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartRequest {

    @NotNull(message = "Ürün ID alanı boş bırakılamaz")
    private Long productId;

    @NotNull(message = "Miktar alanı boş bırakılamaz")
    @Min(value = 1, message = "Eklenen ürün miktarı en az 1 olmalıdır")
    private Integer quantity;
}