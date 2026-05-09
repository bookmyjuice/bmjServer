package com.bookmyjuice.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.entities.WebhookEvent;
import com.bookmyjuice.repositories.WebhookEventRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service for managing webhook event idempotency across all webhook
 * controllers.
 * Uses database-level constraints to ensure proper sequencing and prevent race
 * conditions.
 */
@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);

    @Autowired
    private WebhookEventRepository webhookEventRepo;

    // Cleanup scheduler
    private ScheduledExecutorService scheduler;

    // Keep processed events for 24 hours
    private static final long CLEANUP_HOURS = 24;

    @PostConstruct
    public void init() {
        // Schedule cleanup every 4 hours
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEvents, 4, 4, TimeUnit.HOURS);
        logger.info("IdempotencyService initialized with database-backed storage and cleanup interval of 4 hours");
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    /**
     * Check if an event has already been processed (completed)
     * 
     * @param eventId The event ID to check
     * @return true if the event has been processed, false otherwise
     */
    public boolean isEventProcessed(String eventId) {
        return webhookEventRepo.isEventCompleted(eventId);
    }

    /**
     * Check if an event is currently being processed
     * 
     * @param eventId The event ID to check
     * @return true if the event is being processed, false otherwise
     */
    public boolean isEventProcessing(String eventId) {
        return webhookEventRepo.isEventProcessing(eventId);
    }

    /**
     * Attempt to start processing an event. Creates a database record with unique
     * constraint.
     * 
     * @param eventId   The event ID to start processing
     * @param eventType The type of event
     * @return true if processing started successfully, false if already
     *         processed/processing
     */
    @Transactional
    public boolean startEventProcessing(String eventId, String eventType) {
        try {
            // Try to create a new processing record
            WebhookEvent event = new WebhookEvent(eventId, eventType);
            webhookEventRepo.save(event);
            logger.debug("Started processing event: {} of type: {}", eventId, eventType);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Event already exists - check if it's completed or still processing
            WebhookEvent existing = webhookEventRepo.findByEventId(eventId).orElse(null);
            if (existing != null) {
                if ("COMPLETED".equals(existing.getProcessingStatus())) {
                    logger.debug("Event already completed: {}", eventId);
                    return false; // Already processed
                } else if ("PROCESSING".equals(existing.getProcessingStatus())) {
                    logger.debug("Event already being processed: {}", eventId);
                    return false; // Currently processing
                }
            }
            // Unexpected state
            logger.warn("Unexpected state for event: {}", eventId);
            return false;
        } catch (Exception e) {
            logger.error("Error starting event processing for: {}", eventId, e);
            return false;
        }
    }

    /**
     * Mark an event as completed
     * 
     * @param eventId The event ID to mark as completed
     */
    @Transactional
    public void markEventCompleted(String eventId) {
        int updated = webhookEventRepo.markEventCompleted(eventId, LocalDateTime.now());
        if (updated > 0) {
            logger.debug("Event marked as completed: {}", eventId);
        } else {
            logger.warn("Failed to mark event as completed: {}", eventId);
        }
    }

    /**
     * Mark an event as failed with error message
     * 
     * @param eventId The event ID to mark as failed
     * @param error   The error message
     */
    @Transactional
    public void markEventFailed(String eventId, String error) {
        int updated = webhookEventRepo.markEventFailed(eventId, error, LocalDateTime.now());
        if (updated > 0) {
            logger.debug("Event marked as failed: {} - {}", eventId, error);
        } else {
            logger.warn("Failed to mark event as failed: {}", eventId);
        }
    }

    /**
     * Check if event is processed and mark it as processed if not (legacy method
     * for compatibility)
     * 
     * @param eventId The event ID to check and mark
     * @return true if event was already processed, false if it's new
     * @deprecated Use startEventProcessing and markEventCompleted instead
     */
    @Deprecated
    public boolean checkAndMarkEvent(String eventId) {
        // For backward compatibility, assume generic event type
        if (startEventProcessing(eventId, "GENERIC")) {
            return false; // New event
        } else {
            return isEventProcessed(eventId); // Already processed
        }
    }

    /**
     * Get events that need retry
     * 
     * @return List of events needing retry
     */
    public List<WebhookEvent> getEventsNeedingRetry() {
        return webhookEventRepo.findEventsNeedingRetry();
    }

    /**
     * Retry a failed event
     * 
     * @param event The event to retry
     * @return true if retry was attempted, false otherwise
     */
    @Transactional
    public boolean retryEvent(WebhookEvent event) {
        if (!event.canRetry()) {
            logger.warn("Event {} has exceeded max retry count", event.getEventId());
            return false;
        }

        // Reset to processing state
        event.setProcessingStatus("PROCESSING");
        event.setProcessedAt(LocalDateTime.now());
        webhookEventRepo.save(event);

        logger.info("Retrying event: {} (attempt {})", event.getEventId(), event.getRetryCount() + 1);
        return true;
    }

    /**
     * Remove expired events from the database
     * Events older than CLEANUP_HOURS are removed
     */
    @Transactional
    private void cleanupExpiredEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(CLEANUP_HOURS);
        int removedCount = webhookEventRepo.deleteOldCompletedEvents(cutoff);

        if (removedCount > 0) {
            logger.info("Cleaned up {} old webhook events", removedCount);
        }
    }

    /**
     * Get processing statistics
     * 
     * @return Map with processing statistics
     */
    public ProcessingStats getProcessingStats() {
        List<WebhookEvent> processing = webhookEventRepo.findByProcessingStatus("PROCESSING");
        List<WebhookEvent> failed = webhookEventRepo.findByProcessingStatus("FAILED");
        List<WebhookEvent> completed = webhookEventRepo.findByProcessingStatus("COMPLETED");

        return new ProcessingStats(processing.size(), failed.size(), completed.size());
    }

    /**
     * Statistics class for webhook processing
     */
    public static class ProcessingStats {
        public final int processing;
        public final int failed;
        public final int completed;

        public ProcessingStats(int processing, int failed, int completed) {
            this.processing = processing;
            this.failed = failed;
            this.completed = completed;
        }
    }

}

    if(removedCount>0){logger.info("Cleaned up {} expired event records",removedCount);}

    logger.debug("IdempotencyService cache size: {}",processedEvents.size());}

    /**
     * Get the current number of tracked events
     * 
     * @return Number of events currently being tracked
     */
    public int getTrackedEventCount() {
        return processedEvents.size();
    }

    /**
     * Clear all tracked events (useful for testing)
     */
    public void clearAllEvents() {
        processedEvents.clear();
        logger.warn("All tracked events cleared");
    }
}