package com.ecommerce.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {

    @NotBlank(message = "Ürün adı boş olamaz")
    private String name;

    private String description;

    @NotNull(message = "Fiyat boş olamaz")
    @Positive(message = "Fiyat 0'dan büyük olmalıdır")
    private BigDecimal price;

    @NotNull(message = "Stok miktarı boş olamaz")
    @Min(value = 0, message = "Stok miktarı negatif olamaz")
    private Integer stockQuantity;

    @NotBlank(message = "SKU alanı boş olamaz")
    private String sku;

    @NotNull(message = "Kategori ID'si gereklidir")
    private Long categoryId;
}