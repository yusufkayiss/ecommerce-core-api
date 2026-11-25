package com.ecommerce.api.service;

import com.ecommerce.api.dto.event.OrderCreatedEvent;
import com.ecommerce.api.entity.OutboxEvent;
import com.ecommerce.api.entity.OutboxStatus;
import com.ecommerce.api.producer.OrderEventProducer;
import com.ecommerce.api.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OutboxRelayService {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final Propagator propagator;

    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            OrderEventProducer orderEventProducer,
            ObjectMapper objectMapper) {
        this(outboxEventRepository, orderEventProducer, objectMapper, null, null);
    }

    @Autowired
    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            OrderEventProducer orderEventProducer,
            ObjectMapper objectMapper,
            Tracer tracer,
            Propagator propagator) {
        this.outboxEventRepository = outboxEventRepository;
        this.orderEventProducer = orderEventProducer;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
        this.propagator = propagator;
    }

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
        Map<String, String> traceHeaders = deserializeTraceHeaders(event.getTraceContext());
        Span relaySpan = startRelaySpan(event, traceHeaders);

        try (Tracer.SpanInScope ignored = (relaySpan != null ? tracer.withSpan(relaySpan) : null)) {
            Object deserializedPayload = deserializeEvent(event);
            orderEventProducer.publish(event.getExchangeName(), event.getRoutingKey(), deserializedPayload, traceHeaders);

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setLastError(null);
            outboxEventRepository.save(event);
        } catch (Exception ex) {
            if (relaySpan != null) {
                relaySpan.error(ex);
            }
            int nextRetry = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
            event.setRetryCount(nextRetry);
            event.setLastError(truncateError(ex.getMessage()));

            if (nextRetry >= MAX_RETRIES) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("[OUTBOX FAILED] Outbox event id={} {} deneme sonunda basarisiz oldu: {}",
                        event.getId(), MAX_RETRIES, ex.getMessage());
            } else {
                log.warn("[OUTBOX RETRY] Outbox event id={} gonderilemedi (deneme {}/{}): {}",
                        event.getId(), nextRetry, MAX_RETRIES, ex.getMessage());
            }
            outboxEventRepository.save(event);
        } finally {
            if (relaySpan != null) {
                relaySpan.end();
            }
        }
    }

    private Span startRelaySpan(OutboxEvent event, Map<String, String> traceHeaders) {
        if (tracer == null || propagator == null || traceHeaders.isEmpty()) {
            return null;
        }
        Span.Builder spanBuilder = propagator.extract(traceHeaders, Map::get);
        return spanBuilder
                .name("outbox-relay " + event.getEventType())
                .kind(Span.Kind.PRODUCER)
                .tag("outbox.event.id", String.valueOf(event.getId()))
                .tag("outbox.aggregate.id", event.getAggregateId())
                .start();
    }

    private Object deserializeEvent(OutboxEvent event) throws Exception {
        if ("OrderCreatedEvent".equals(event.getEventType())) {
            return objectMapper.readValue(event.getPayload(), OrderCreatedEvent.class);
        }
        return event.getPayload();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> deserializeTraceHeaders(String traceContextJson) {
        if (traceContextJson == null || traceContextJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(traceContextJson, Map.class);
        } catch (Exception e) {
            log.warn("Outbox traceContext parse edilemedi: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String truncateError(String message) {
        if (message == null) {
            return "Bilinmeyen hata";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}