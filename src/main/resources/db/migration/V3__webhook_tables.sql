-- ============================================================
-- Migration V3: Webhook Reliability
-- BookMyJuice Enterprise — Idempotent webhook processing
-- ============================================================

-- Webhook Events: Idempotent event tracking
CREATE TABLE IF NOT EXISTS webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    status ENUM('PROCESSING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PROCESSING',
    payload JSON,
    error_message TEXT,
    attempts INT NOT NULL DEFAULT 1,
    max_attempts INT NOT NULL DEFAULT 5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    UNIQUE KEY uk_event_id (event_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    INDEX idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Webhook Dead Letter Queue: Events that exhausted retries
CREATE TABLE IF NOT EXISTS webhook_dlq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON,
    error_message TEXT,
    last_error TEXT,
    failed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    max_attempts INT NOT NULL,
    attempts_made INT NOT NULL,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP NULL,
    resolution_notes TEXT,
    INDEX idx_unresolved (resolved, failed_at),
    INDEX idx_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
