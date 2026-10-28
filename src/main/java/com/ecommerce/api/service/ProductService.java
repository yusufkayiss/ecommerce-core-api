package com.ecommerce.api.service;

import com.ecommerce.api.entity.Product;
import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product createProduct(Product product);
    Product getProductById(Long id);
    Product updateProduct(Long id, Product productDetails);
    void deleteProduct(Long id);
    List<Product> getProductsByCategoryId(Long categoryId);
}