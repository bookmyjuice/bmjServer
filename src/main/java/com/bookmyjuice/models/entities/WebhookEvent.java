package com.bookmyjuice.models.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Entity for tracking processed webhook events to ensure idempotency and
 * sequencing.
 * Uses database-level constraints to prevent race conditions across multiple
 * instances.
 */
@Entity
@Table(name = "webhook_events")
public class WebhookEvent {

    @Id
    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "processing_status", nullable = false)
    private String processingStatus; // "PROCESSING", "COMPLETED", "FAILED"

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_error")
    private String lastError;

    @Version
    private Long version;

    // Constructors
    public WebhookEvent() {
    }

    public WebhookEvent(String eventId, String eventType) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = LocalDateTime.now();
        this.processingStatus = "PROCESSING";
        this.retryCount = 0;
    }

    // Getters and setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Mark event as completed
     */
    public void markCompleted() {
        this.processingStatus = "COMPLETED";
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Mark event as failed with error message
     */
    public void markFailed(String error) {
        this.processingStatus = "FAILED";
        this.lastError = error;
        this.retryCount++;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Check if event can be retried
     */
    public boolean canRetry() {
        return retryCount < 3; // Max 3 retries
    }
}