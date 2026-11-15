package com.bookmyjuice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.chargebee.models.Event;

/**
 * Runs the {@link IdentityLinker} for a Chargebee webhook <em>after</em> the mirror write
 * has completed.
 *
 * <p>Two deliberate properties:
 * <ul>
 *   <li><b>Not transactional.</b> It is invoked by the webhook controllers after the
 *       processor has returned, so a linking failure can never roll back billing mirror
 *       data. Chargebee remains the source of truth even if the link is not made.</li>
 *   <li><b>Exception-swallowing</b> (with logging). A webhook must not fail because
 *       identity linking failed; the reconciliation jobs of Phase 4 repair anything
 *       missed, and a failed webhook would be retried for the wrong reason.</li>
 * </ul>
 *
 * <p>References: CHARGEBEE_SYNC_ARCHITECTURE.md §3 and §7, CHARGEBEE_SYNC_ROADMAP.md Phase 2.
 */
@Service
public class WebhookIdentityHook {

    private static final Logger logger = LoggerFactory.getLogger(WebhookIdentityHook.class);

    @Autowired
    private IdentityLinker identityLinker;

    /**
     * Extract the customer id from the event and attempt a link.
     *
     * @param event a hydrated Chargebee event; may be {@code null}
     */
    public void afterMirroring(Event event) {
        if (event == null) {
            return;
        }
        String customerId;
        try {
            customerId = event.content() != null && event.content().customer() != null
                    ? event.content().customer().id()
                    : null;
        } catch (RuntimeException e) {
            // Event types without a customer content block: nothing to link.
            logger.debug("No customer content on event; identity link skipped: {}", e.getMessage());
            return;
        }
        linkForCustomer(customerId, safeEventId(event));
    }

    /**
     * Link a customer that a webhook just mirrored. Testable core: no Chargebee types.
     *
     * @param customerId Chargebee customer id, may be {@code null}
     * @param eventId    event id for log correlation, may be {@code null}
     */
    public void linkForCustomer(String customerId, String eventId) {
        if (customerId == null || customerId.isBlank()) {
            return;
        }
        try {
            IdentityLinker.LinkResult result = identityLinker.link(customerId);
            if (result.isLinked()) {
                logger.info("Identity link after webhook {}: {} for customer '{}'",
                        eventId, result.getOutcome(), customerId);
            } else {
                logger.warn("Identity link not created after webhook {}: {} for customer '{}'",
                        eventId, result, customerId);
            }
        } catch (RuntimeException e) {
            logger.error("Identity link attempt failed for customer '{}' (event {}): {}",
                    customerId, eventId, e.getMessage(), e);
        }
    }

    private String safeEventId(Event event) {
        try {
            return event.id();
        } catch (RuntimeException e) {
            return "unknown";
        }
    }
}
