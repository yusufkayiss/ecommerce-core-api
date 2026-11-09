package com.ecommerce.api.service.impl;

import com.ecommerce.api.dto.request.CreateProductRequest;
import com.ecommerce.api.dto.response.ProductResponse;
import com.ecommerce.api.entity.Category;
import com.ecommerce.api.entity.Product;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.CategoryRepository;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"products", "products_by_category"}, allEntries = true)
    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Bu SKU koduna sahip bir ürün zaten mevcut: " + request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .sku(request.getSku())
                .category(category)
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "product", key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products_by_category", key = "#categoryId")
    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByCategoryId(categoryId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"product", "products", "products_by_category"}, allEntries = true)
    public ProductResponse updateProduct(Long id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı: " + id));

        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new RuntimeException("Bu SKU koduna sahip başka bir ürün mevcut: " + request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Kategori bulunamadı: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setSku(request.getSku());
        product.setCategory(category);

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"product", "products", "products_by_category"}, allEntries = true)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ürün bulunamadı: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .sku(product.getSku())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}