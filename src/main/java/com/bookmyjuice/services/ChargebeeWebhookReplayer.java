package com.bookmyjuice.services;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.chargebee.models.Event;

/**
 * Production {@link WebhookEventReplayer}: re-fetches the event from Chargebee and routes
 * it to the same processor methods the live controllers use.
 *
 * <p>Re-fetching (rather than trusting a stored payload) keeps Chargebee as the source of
 * truth even during a replay of an old event.
 *
 * <p>References: CHARGEBEE_SYNC_ROADMAP.md Phase 3.
 */
@Component
public class ChargebeeWebhookReplayer implements WebhookEventReplayer {

    private static final Logger logger = LoggerFactory.getLogger(ChargebeeWebhookReplayer.class);

    @Autowired
    private WebhookEventProcessor processor;

    @Override
    public boolean replay(String eventId, String eventType) throws Exception {
        Event event = Event.retrieve(eventId).request().event();
        String type = resolveType(event, eventType);
        ResponseEntity<?> response = dispatch(event, type);
        boolean ok = response != null && response.getStatusCode().is2xxSuccessful();
        logger.info("Replayed event {} of type '{}' -> {}", eventId, type, ok ? "ok" : "failed");
        return ok;
    }

    private String resolveType(Event event, String fallback) {
        try {
            if (event != null && event.eventType() != null) {
                return event.eventType().name().toLowerCase(Locale.ROOT);
            }
        } catch (RuntimeException e) {
            logger.debug("Could not read event type from Chargebee event: {}", e.getMessage());
        }
        return fallback == null ? "" : fallback.toLowerCase(Locale.ROOT);
    }

    private ResponseEntity<?> dispatch(Event event, String type) {
        if (type.startsWith("customer")) {
            return processor.processCustomerEvent(event);
        }
        if (type.startsWith("subscription")) {
            return processor.processSubscriptionEvent(event);
        }
        if (type.startsWith("item_price")) {
            return processor.processItemPriceEvent(event);
        }
        if (type.startsWith("item")) {
            return processor.processItemEvent(event);
        }
        if (type.startsWith("invoice")) {
            return processor.processInvoiceEvent(event);
        }
        if (type.startsWith("payment") || type.startsWith("transaction")) {
            return processor.processPaymentEvent(event);
        }
        logger.warn("No handler for replayed event type '{}'; treating as handled", type);
        return ResponseEntity.ok("unhandled event type");
    }
}
