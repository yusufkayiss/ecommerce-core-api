package com.ecommerce.api.service;

import com.ecommerce.api.dto.event.OrderCreatedEvent;
import com.ecommerce.api.entity.OutboxEvent;
import com.ecommerce.api.entity.OutboxStatus;
import com.ecommerce.api.producer.OrderEventProducer;
import com.ecommerce.api.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayService {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:2000}")
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            relaySingleEvent(event);
        }
    }

    public void relaySingleEvent(OutboxEvent event) {
        try {
            Object deserializedPayload = deserializeEvent(event);
            orderEventProducer.publish(event.getExchangeName(), event.getRoutingKey(), deserializedPayload);

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setLastError(null);
            outboxEventRepository.save(event);
        } catch (Exception ex) {
            int nextRetry = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
            event.setRetryCount(nextRetry);
            event.setLastError(truncateError(ex.getMessage()));

            if (nextRetry >= MAX_RETRIES) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("[OUTBOX FAILED] Outbox event id={} {} deneme sonunda başarısız oldu: {}",
                        event.getId(), MAX_RETRIES, ex.getMessage());
            } else {
                log.warn("[OUTBOX RETRY] Outbox event id={} gönderilemedi (deneme {}/{}): {}",
                        event.getId(), nextRetry, MAX_RETRIES, ex.getMessage());
            }
            outboxEventRepository.save(event);
        }
    }

    private Object deserializeEvent(OutboxEvent event) throws Exception {
        if ("OrderCreatedEvent".equals(event.getEventType())) {
            return objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class);
        }
        return event.getPayload();
    }

    private String truncateError(String message) {
        if (message == null) {
            return "Bilinmeyen hata";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}