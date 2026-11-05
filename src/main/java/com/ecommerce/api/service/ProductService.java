package com.ecommerce.api.service;

import com.ecommerce.api.dto.request.CreateProductRequest;
import com.ecommerce.api.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse getProductById(Long id);

    List<ProductResponse> getAllProducts();

    List<ProductResponse> getProductsByCategoryId(Long categoryId);

    ProductResponse updateProduct(Long id, CreateProductRequest request);

    void deleteProduct(Long id);
}