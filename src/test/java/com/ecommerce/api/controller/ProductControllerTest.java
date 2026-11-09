package com.ecommerce.api.controller;

import com.ecommerce.api.config.JwtService;
import com.ecommerce.api.dto.request.CreateProductRequest;
import com.ecommerce.api.dto.response.ProductResponse;
import com.ecommerce.api.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("GET /api/v1/products - Tüm ürünleri listeleyip 200 OK dönmeli")
    void getAllProducts_ShouldReturnOk() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .price(new BigDecimal("25000.00"))
                .sku("LAP-12345")
                .build();

        when(productService.getAllProducts()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].sku").value("LAP-12345"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Tek ürün detayını ve 200 OK dönmeli")
    void getProductById_ShouldReturnProduct() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .price(new BigDecimal("25000.00"))
                .sku("LAP-12345")
                .build();

        when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @DisplayName("POST /api/v1/products - Yeni ürün oluşturup 201 Created dönmeli")
    void createProduct_ShouldReturnCreated() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Laptop")
                .description("Yüksek performanslı")
                .price(new BigDecimal("25000.00"))
                .stockQuantity(10)
                .sku("LAP-12345")
                .categoryId(1L)
                .build();

        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .sku("LAP-12345")
                .build();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sku").value("LAP-12345"));
    }
}