package com.ecommerce.api.service;

import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.IdempotencyConflictException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private static final String IN_PROGRESS_VALUE = "IN_PROGRESS";
    private static final Duration LOCK_TTL = Duration.ofMinutes(2);
    private static final Duration RESPONSE_TTL = Duration.ofHours(24);
    private static final int MAX_KEY_LENGTH = 128;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T execute(String scope, Long userId, String idempotencyKey, Class<T> responseType, Supplier<T> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        String normalizedKey = idempotencyKey.trim();
        if (normalizedKey.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException("Idempotency-Key en fazla " + MAX_KEY_LENGTH + " karakter olabilir.");
        }

        String redisKey = String.format("idempotency:%s:%d:%s", scope, userId, normalizedKey);
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, IN_PROGRESS_VALUE, LOCK_TTL);

        if (Boolean.FALSE.equals(acquired)) {
            String cachedValue = stringRedisTemplate.opsForValue().get(redisKey);
            if (cachedValue == null || IN_PROGRESS_VALUE.equals(cachedValue)) {
                throw new IdempotencyConflictException("Bu istek şu anda işleniyor, lütfen tekrar denemeyin.");
            }
            log.info("Idempotency cache hit for key={}", redisKey);
            return deserialize(cachedValue, responseType);
        }

        try {
            T result = action.get();
            stringRedisTemplate.opsForValue().set(redisKey, serialize(result), RESPONSE_TTL);
            return result;
        } catch (RuntimeException ex) {
            stringRedisTemplate.delete(redisKey);
            throw ex;
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Idempotency yanıtı JSON formatına dönüştürülemedi", e);
        }
    }

    private <T> T deserialize(String json, Class<T> responseType) {
        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Önbellekteki idempotency yanıtı okunamadı", e);
        }
    }
}