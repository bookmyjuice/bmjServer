package com.bookmyjuice.services;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service for managing webhook event idempotency across all webhook controllers.
 * Ensures that the same event ID is not processed multiple times.
 */
@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    
    // Store event ID with timestamp for cleanup
    private final ConcurrentHashMap<String, LocalDateTime> processedEvents = new ConcurrentHashMap<>();
    
    // Cleanup scheduler
    private ScheduledExecutorService scheduler;
    
    // Keep processed events for 24 hours
    private static final long CLEANUP_HOURS = 24;
    
    @PostConstruct
    public void init() {
        // Schedule cleanup every 4 hours
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEvents, 4, 4, TimeUnit.HOURS);
        logger.info("IdempotencyService initialized with cleanup interval of 4 hours");
    }
    
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
    
    /**
     * Check if an event has already been processed
     * @param eventId The event ID to check
     * @return true if the event has been processed, false otherwise
     */
    public boolean isEventProcessed(String eventId) {
        return processedEvents.containsKey(eventId);
    }
    
    /**
     * Mark an event as processed
     * @param eventId The event ID to mark as processed
     */
    public void markEventAsProcessed(String eventId) {
        processedEvents.put(eventId, LocalDateTime.now());
        logger.debug("Event marked as processed: {}", eventId);
    }
    
    /**
     * Check if event is processed and mark it as processed if not
     * @param eventId The event ID to check and mark
     * @return true if event was already processed, false if it's new
     */
    public boolean checkAndMarkEvent(String eventId) {
        LocalDateTime timestamp = processedEvents.putIfAbsent(eventId, LocalDateTime.now());
        boolean wasProcessed = timestamp != null;
        
        if (wasProcessed) {
            logger.debug("Event already processed: {}", eventId);
        } else {
            logger.debug("Event marked as processed: {}", eventId);
        }
        
        return wasProcessed;
    }
    
    /**
     * Remove expired events from the cache
     * Events older than CLEANUP_HOURS are removed
     */
    private void cleanupExpiredEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(CLEANUP_HOURS);
        int removedCount = 0;
        
        var iterator = processedEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isBefore(cutoff)) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("Cleaned up {} expired event records", removedCount);
        }
        
        logger.debug("IdempotencyService cache size: {}", processedEvents.size());
    }
    
    /**
     * Get the current number of tracked events
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