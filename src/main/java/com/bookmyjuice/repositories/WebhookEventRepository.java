package com.bookmyjuice.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.WebhookEvent;

import jakarta.transaction.Transactional;

/**
 * Repository for WebhookEvent entity to handle database-level idempotency and
 * sequencing.
 */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, String> {

    /**
     * Find webhook event by ID
     */
    Optional<WebhookEvent> findByEventId(String eventId);

    /**
     * Check if event exists and is completed
     */
    @Query("SELECT COUNT(we) > 0 FROM WebhookEvent we WHERE we.eventId = :eventId AND we.processingStatus = 'COMPLETED'")
    boolean isEventCompleted(@Param("eventId") String eventId);

    /**
     * Check if event exists and is processing
     */
    @Query("SELECT COUNT(we) > 0 FROM WebhookEvent we WHERE we.eventId = :eventId AND we.processingStatus = 'PROCESSING'")
    boolean isEventProcessing(@Param("eventId") String eventId);

    /**
     * Mark event as completed
     */
    @Modifying
    @Transactional
    @Query("UPDATE WebhookEvent we SET we.processingStatus = 'COMPLETED', we.processedAt = :processedAt WHERE we.eventId = :eventId")
    int markEventCompleted(@Param("eventId") String eventId, @Param("processedAt") LocalDateTime processedAt);

    /**
     * Mark event as failed
     */
    @Modifying
    @Transactional
    @Query("UPDATE WebhookEvent we SET we.processingStatus = 'FAILED', we.lastError = :error, we.retryCount = we.retryCount + 1, we.processedAt = :processedAt WHERE we.eventId = :eventId")
    int markEventFailed(@Param("eventId") String eventId, @Param("error") String error,
            @Param("processedAt") LocalDateTime processedAt);

    /**
     * Find events that need retry (failed and retry count < 3)
     */
    @Query("SELECT we FROM WebhookEvent we WHERE we.processingStatus = 'FAILED' AND we.retryCount < 3")
    List<WebhookEvent> findEventsNeedingRetry();

    /**
     * Clean up old completed events (older than specified hours)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM WebhookEvent we WHERE we.processingStatus = 'COMPLETED' AND we.processedAt < :cutoffDate")
    int deleteOldCompletedEvents(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Get events by status
     */
    List<WebhookEvent> findByProcessingStatus(String processingStatus);

    /**
     * Get events by type and status
     */
    List<WebhookEvent> findByEventTypeAndProcessingStatus(String eventType, String processingStatus);
}