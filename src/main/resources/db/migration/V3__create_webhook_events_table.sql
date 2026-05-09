-- Create webhook_events table for database-level idempotency and sequencing
-- This prevents race conditions in webhook processing across multiple instances

CREATE TABLE webhook_events (
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    processing_status VARCHAR(20) NOT NULL CHECK (processing_status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (event_id)
);

-- Index for efficient querying by status and type
CREATE INDEX idx_webhook_events_status ON webhook_events(processing_status);
CREATE INDEX idx_webhook_events_type_status ON webhook_events(event_type, processing_status);
CREATE INDEX idx_webhook_events_processed_at ON webhook_events(processed_at);

-- Index for cleanup operations
CREATE INDEX idx_webhook_events_cleanup ON webhook_events(processing_status, processed_at);