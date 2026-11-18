package com.ecommerce.api.integration;

import com.ecommerce.api.AbstractIntegrationTest;
import com.ecommerce.api.config.RabbitMQConfig;
import com.ecommerce.api.dto.request.AddToCartRequest;
import com.ecommerce.api.dto.request.RegisterRequest;
import com.ecommerce.api.dto.response.AuthenticationResponse;
import com.ecommerce.api.entity.Category;
import com.ecommerce.api.entity.OutboxEvent;
import com.ecommerce.api.entity.OutboxStatus;
import com.ecommerce.api.entity.Product;
import com.ecommerce.api.repository.CategoryRepository;
import com.ecommerce.api.repository.OutboxEventRepository;
import com.ecommerce.api.repository.ProductRepository;
import com.ecommerce.api.service.OutboxRelayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "outbox.relay.fixed-delay-ms=600000"
})
@AutoConfigureMockMvc
class OrderEndToEndIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxRelayService outboxRelayService;

    // RabbitTemplate'i Spy olarak işaretliyoruz. Gerçek metod çalışır, ancak çağrıları yakalayabiliriz.
    @SpyBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("Flyway + Auth + Cart + Idempotent Order + Outbox Relay -> RabbitMQ uçtan uca akışı doğrulanmalı")
    void shouldExecuteFullOrderFlowWithIdempotencyAndOutboxDelivery() throws Exception {
        // 1. Kullanıcı kaydı ve JWT alımı
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Yusuf")
                .lastName("Kayis")
                .email("e2e-" + UUID.randomUUID() + "@ecommerce.com")
                .password("SecurePass123!")
                .build();

        MvcResult authResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthenticationResponse authResponse = objectMapper.readValue(
                authResult.getResponse().getContentAsString(),
                AuthenticationResponse.class
        );
        String bearerToken = "Bearer " + authResponse.getToken();

        // 2. Kategori ve ürün hazırlığı
        Category category = categoryRepository.save(Category.builder()
                .name("Electronics-" + UUID.randomUUID())
                .description("Test electronics")
                .build());

        Product product = productRepository.save(Product.builder()
                .name("Enterprise Laptop")
                .description("High performance workstation")
                .price(new BigDecimal("1500.00"))
                .stockQuantity(10)
                .sku("SKU-" + UUID.randomUUID())
                .category(category)
                .build());

        // 3. Sepete 2 adet ürün ekle
        AddToCartRequest cartRequest = new AddToCartRequest();
        cartRequest.setProductId(product.getId());
        cartRequest.setQuantity(2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        // 4. Idempotency-Key ile ilk sipariş oluşturma
        String idempotencyKey = "idem-e2e-" + UUID.randomUUID();

        MvcResult firstOrderResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode firstOrderJson = objectMapper.readTree(firstOrderResult.getResponse().getContentAsString());
        long createdOrderId = firstOrderJson.get("id").asLong();

        // Stok 10 -> 8 düşmüş olmalı
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(8);

        // Outbox tablosunda PENDING kayıt oluşmuş olmalı
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        assertThat(pendingEvents).anyMatch(e -> e.getAggregateId().equals(String.valueOf(createdOrderId)));

        // 5. Aynı Idempotency-Key ile tekrar sipariş isteği (mükerrer işlem koruması)
        MvcResult duplicateOrderResult = mockMvc.perform(post("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .header("Idempotency-Key", idempotencyKey))
                .andReturn();

        int duplicateStatus = duplicateOrderResult.getResponse().getStatus();
        assertThat(duplicateStatus).isIn(200, 201);
        JsonNode duplicateOrderJson = objectMapper.readTree(duplicateOrderResult.getResponse().getContentAsString());
        assertThat(duplicateOrderJson.get("id").asLong()).isEqualTo(createdOrderId);

        // Stok tekrar düşmemiş olmalı (hâlâ 8)
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(8);

        // 6. Outbox Relay çalıştır
        outboxRelayService.processPendingEvents();

        // Veritabanında PENDING kalmadığını (COMPLETED'a çekildiğini) doğrula
        List<OutboxEvent> remainingPending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        assertThat(remainingPending).noneMatch(e -> e.getAggregateId().equals(String.valueOf(createdOrderId)));

        // 7. SpyBean üzerinden RabbitMQ'ya mesajın gerçekten atıldığını doğrula.
        // Consumer çalışıp mesajı tüketebilir, sorun değil. Biz "gönderim" işlemini yakalıyoruz.
        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        
        verify(rabbitTemplate, timeout(5000).atLeastOnce()).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_ORDER),
                eq(RabbitMQConfig.ROUTING_KEY_ORDER_CREATED),
                messageCaptor.capture()
        );

        // Gönderilen mesajın içeriğini (payload) kontrol et
        Object sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).isNotNull();
        String payloadString = sentMessage.toString();
        
        // Mesaj payload'u (JSON veya nesne fark etmez) Order ID ve tutarı barındırmalı
        assertThat(payloadString).contains(String.valueOf(createdOrderId));
        assertThat(payloadString).contains("3000.00");
    }
}