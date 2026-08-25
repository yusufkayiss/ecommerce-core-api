package com.ecommerce.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ürün adı boş olamaz!")
    @Column(nullable = false)
    private String name;

    private String description;

    @NotNull(message = "Fiyat bilgisi zorunludur!")
    @Positive(message = "Fiyat 0'dan büyük olmalıdır!")
    @Column(nullable = false)
    private BigDecimal price;

    @NotNull(message = "Stok miktarı zorunludur!")
    @Min(value = 0, message = "Stok miktarı negatif olamaz!")
    private Integer stockQuantity;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}