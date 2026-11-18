-- V2: Transactional Outbox Pattern Table

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    exchange_name VARCHAR(150) NOT NULL,
    routing_key VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created_at
    ON outbox_events (status, created_at);