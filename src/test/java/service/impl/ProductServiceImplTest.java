package service.impl;

import com.ecommerce.api.dto.request.CreateProductRequest;
import com.ecommerce.api.dto.response.ProductResponse;
import com.ecommerce.api.entity.Category;
import com.ecommerce.api.entity.Product;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.CategoryRepository;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category category;
    private Product product;
    private CreateProductRequest createRequest;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Elektronik")
                .build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("Yüksek performanslı bilgisayar")
                .price(new BigDecimal("25000.00"))
                .stockQuantity(10)
                .sku("LAP-12345")
                .category(category)
                .build();

        createRequest = CreateProductRequest.builder()
                .name("Laptop")
                .description("Yüksek performanslı bilgisayar")
                .price(new BigDecimal("25000.00"))
                .stockQuantity(10)
                .sku("LAP-12345")
                .categoryId(1L)
                .build();
    }

    @Test
    @DisplayName("getProductById - Ürün bulunduğunda DTO dönmeli")
    void getProductById_WhenProductExists_ShouldReturnProductResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Laptop");
        assertThat(response.getSku()).isEqualTo("LAP-12345");
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getProductById - Ürün bulunamadığında ResourceNotFoundException fırlatmalı")
    void getProductById_WhenProductNotFound_ShouldThrowException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ürün bulunamadı: 99");

        verify(productRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("getAllProducts - Tüm ürün listesini dönmeli")
    void getAllProducts_ShouldReturnListOfProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getAllProducts();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Laptop");
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("createProduct - Başarılı ürün kaydı")
    void createProduct_WhenValidRequest_ShouldSaveAndReturnResponse() {
        when(productRepository.existsBySku(createRequest.getSku())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getSku()).isEqualTo("LAP-12345");
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct - SKU zaten mevcutsa Exception fırlatmalı")
    void createProduct_WhenSkuAlreadyExists_ShouldThrowException() {
        when(productRepository.existsBySku(createRequest.getSku())).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(createRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bu SKU koduna sahip bir ürün zaten mevcut");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("deleteProduct - Ürün varsa başarıyla silmeli")
    void deleteProduct_WhenProductExists_ShouldDelete() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository, times(1)).deleteById(1L);
    }
}