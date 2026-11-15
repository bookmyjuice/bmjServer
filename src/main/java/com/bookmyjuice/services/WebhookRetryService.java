package com.bookmyjuice.services;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.entities.WebhookEvent;
import com.bookmyjuice.repository.WebhookEventRepository;

/**
 * Drains webhook events that failed to process.
 *
 * <p>The retry capability already existed in the codebase (a query for
 * {@code FAILED AND retryCount < 3} and a {@code retryEvent} helper) but nothing ever
 * invoked it, so a handler failure was permanent unless Chargebee happened to redeliver.
 * This worker closes that gap: every {@code webhook.retry.interval-ms} it tries each
 * failed event again, and dead-letters anything that exhausts its attempts.
 *
 * <p>Deliberately not transactional as a whole: each outcome is written through
 * {@link IdempotencyService}, so one bad event cannot roll back the progress of the others.
 *
 * <p>References: CHARGEBEE_SYNC_ROADMAP.md Phase 3, CHARGEBEE_WEBHOOK_MATRIX.md.
 */
@Service
public class WebhookRetryService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookRetryService.class);

    /** Attempts before an event becomes operator-facing dead letter. */
    static final int MAX_ATTEMPTS = 3;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private WebhookEventReplayer replayer;

    /** Scheduled entry point. Interval and initial delay are configurable. */
    @Scheduled(
            fixedDelayString = "${webhook.retry.interval-ms:300000}",
            initialDelayString = "${webhook.retry.initial-delay-ms:60000}")
    public void scheduledDrain() {
        RetryReport report = drainOnce();
        if (report.getRetried() > 0) {
            logger.info("Webhook retry drain: {}", report);
        }
    }

    /**
     * One pass over the retry queue. Public and free of scheduling so it can be tested and
     * invoked by an operator.
     */
    public RetryReport drainOnce() {
        List<WebhookEvent> candidates;
        try {
            candidates = webhookEventRepository.findEventsNeedingRetry();
        } catch (RuntimeException e) {
            logger.error("Could not read the webhook retry queue: {}", e.getMessage(), e);
            return new RetryReport(0, 0, 0, 0);
        }
        if (candidates == null || candidates.isEmpty()) {
            return new RetryReport(0, 0, 0, 0);
        }

        int retried = 0;
        int completed = 0;
        int failed = 0;
        int deadLettered = 0;

        for (WebhookEvent event : candidates) {
            retried++;
            String eventId = event.getEventId();
            boolean ok = false;
            String error = null;
            try {
                ok = replayer.replay(eventId, event.getEventType());
            } catch (Exception e) {
                error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                logger.warn("Replay of event {} threw: {}", eventId, error);
            }

            if (ok) {
                idempotencyService.markEventCompleted(eventId);
                completed++;
                continue;
            }

            String reason = error != null ? error : "handler reported failure";
            idempotencyService.markEventFailed(eventId, reason);
            failed++;

            int attempts = (event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1;
            if (attempts >= MAX_ATTEMPTS) {
                idempotencyService.markEventDeadLettered(eventId, reason);
                deadLettered++;
                logger.error("Event {} dead-lettered after {} attempts: {}", eventId, attempts, reason);
            }
        }

        return new RetryReport(retried, completed, failed, deadLettered);
    }

    /** Outcome of one drain pass. */
    public static final class RetryReport {
        private final int retried;
        private final int completed;
        private final int failed;
        private final int deadLettered;

        RetryReport(int retried, int completed, int failed, int deadLettered) {
            this.retried = retried;
            this.completed = completed;
            this.failed = failed;
            this.deadLettered = deadLettered;
        }

        public int getRetried() {
            return retried;
        }

        public int getCompleted() {
            return completed;
        }

        public int getFailed() {
            return failed;
        }

        public int getDeadLettered() {
            return deadLettered;
        }

        @Override
        public String toString() {
            return "retried=" + retried + ", completed=" + completed
                    + ", failed=" + failed + ", deadLettered=" + deadLettered;
        }
    }

    /** Timestamp helper kept for symmetry with the repository signatures. */
    static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
