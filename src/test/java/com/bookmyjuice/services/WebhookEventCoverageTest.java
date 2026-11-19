package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Asserts that every event type the live Chargebee site actually subscribes has a handler.
 *
 * <p>The list below is the union measured from
 * {@code GET /api/v2/webhook_endpoints} on bookmyjuice-test (see CHARGEBEE_LIVE_AUDIT.md).
 * If someone adds a subscription in the dashboard without a handler, or removes a handler
 * case, this test fails — which is the point.
 */
class WebhookEventCoverageTest {

    /** Exact union of enabled_events across the five configured endpoints (29 types). */
    private static final List<String> SUBSCRIBED_IN_CHARGEBEE = List.of(
            // subscription endpoint (disabled)
            "subscription_created", "subscription_cancelled", "subscription_paused",
            "subscription_resumed", "subscription_activated", "subscription_started",
            "subscription_changed", "subscription_reactivated", "subscription_renewed",
            "subscription_deleted",
            // customer endpoint (disabled)
            "customer_created", "customer_changed", "customer_deleted",
            "customer_entitlements_updated",
            // item endpoint
            "item_created", "item_updated", "item_deleted",
            "item_price_created", "item_price_updated", "item_price_deleted",
            // itemPrices endpoint
            // (same three item_price_* types, already listed above)
            // order endpoint
            "order_created", "order_updated", "order_cancelled", "order_delivered",
            "order_returned", "order_ready_to_process", "order_ready_to_ship",
            "order_deleted", "order_resent");

    @Test
    @DisplayName("every event type subscribed in Chargebee has a handler")
    void everySubscribedEventIsCovered() {
        List<String> uncovered = SUBSCRIBED_IN_CHARGEBEE.stream()
                .filter(t -> !WebhookEventCoverage.isCovered(t))
                .toList();

        assertTrue(uncovered.isEmpty(),
                "Chargebee subscribes these types but nothing handles them: " + uncovered);
        assertEquals(29, SUBSCRIBED_IN_CHARGEBEE.size(),
                "the audit measured 29 distinct subscribed event types");
    }

    @Test
    @DisplayName("the three previously missing subscription types now route to SUBSCRIPTION")
    void newlyAddedSubscriptionTypesRoute() {
        for (String type : List.of("subscription_activated", "subscription_started", "subscription_deleted")) {
            assertEquals(WebhookEventCoverage.Route.SUBSCRIPTION, WebhookEventCoverage.routeFor(type),
                    type + " must be handled by the subscription processor");
        }
    }

    @Test
    @DisplayName("item price events are routed to the item-price handler, not the item handler")
    void itemPriceIsNotItem() {
        assertEquals(WebhookEventCoverage.Route.ITEM_PRICE,
                WebhookEventCoverage.routeFor("item_price_updated"));
        assertEquals(WebhookEventCoverage.Route.ITEM, WebhookEventCoverage.routeFor("item_updated"));
    }

    @Test
    @DisplayName("payment and invoice handlers exist even though no endpoint subscribes them yet")
    void invoiceAndPaymentHandlersExist() {
        assertTrue(WebhookEventCoverage.isCovered("payment_failed"));
        assertTrue(WebhookEventCoverage.isCovered("invoice_generated"));
    }

    @Test
    @DisplayName("unknown and null types are reported as unhandled, never assumed")
    void unknownIsUnhandled() {
        assertFalse(WebhookEventCoverage.isCovered("subscription.teleported"));
        assertFalse(WebhookEventCoverage.isCovered(""));
        assertFalse(WebhookEventCoverage.isCovered(null));
        assertEquals(WebhookEventCoverage.Route.UNHANDLED,
                WebhookEventCoverage.routeFor("something_unknown"));
    }

    @Test
    @DisplayName("matching is case-insensitive and tolerant of whitespace")
    void normalisation() {
        assertTrue(WebhookEventCoverage.isCovered("SUBSCRIPTION_CREATED"));
        assertTrue(WebhookEventCoverage.isCovered("  subscription_created  "));
    }
}
