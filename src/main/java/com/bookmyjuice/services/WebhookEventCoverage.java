package com.bookmyjuice.services;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Which component handles which Chargebee event type.
 *
 * <p>This exists because the coverage question — "does every event the Chargebee
 * subscription actually sends have a handler?" — could not be answered before without
 * reading thirteen controllers and one switch statement. It is the single place to check,
 * and {@code WebhookEventCoverageTest} asserts it against the live audit of subscribed
 * events in {@code CHARGEBEE_LIVE_AUDIT.md}.
 *
 * <p>A type absent from this map is a delivery that will be rejected and retried forever.
 */
public final class WebhookEventCoverage {

    /** Component that consumes an event type. */
    public enum Route {
        CUSTOMER,
        SUBSCRIPTION,
        ITEM,
        ITEM_PRICE,
        INVOICE,
        PAYMENT,
        ORDER,
        UNHANDLED
    }

    private static final Map<String, Route> ROUTES;

    static {
        Map<String, Route> m = new LinkedHashMap<>();

        // --- customer endpoint (currently disabled in Chargebee) ---
        m.put("customer_created", Route.CUSTOMER);
        m.put("customer_changed", Route.CUSTOMER);
        m.put("customer_deleted", Route.CUSTOMER);
        m.put("customer_entitlements_updated", Route.CUSTOMER);

        // --- subscription endpoint (currently disabled in Chargebee) ---
        m.put("subscription_created", Route.SUBSCRIPTION);
        m.put("subscription_changed", Route.SUBSCRIPTION);
        m.put("subscription_cancelled", Route.SUBSCRIPTION);
        m.put("subscription_paused", Route.SUBSCRIPTION);
        m.put("subscription_resumed", Route.SUBSCRIPTION);
        m.put("subscription_reactivated", Route.SUBSCRIPTION);
        m.put("subscription_renewed", Route.SUBSCRIPTION);
        m.put("subscription_activated", Route.SUBSCRIPTION);
        m.put("subscription_started", Route.SUBSCRIPTION);
        m.put("subscription_deleted", Route.SUBSCRIPTION);

        // --- item + item price endpoints ---
        m.put("item_created", Route.ITEM);
        m.put("item_updated", Route.ITEM);
        m.put("item_deleted", Route.ITEM);
        m.put("item_price_created", Route.ITEM_PRICE);
        m.put("item_price_updated", Route.ITEM_PRICE);
        m.put("item_price_deleted", Route.ITEM_PRICE);

        // --- order endpoint ---
        m.put("order_created", Route.ORDER);
        m.put("order_updated", Route.ORDER);
        m.put("order_cancelled", Route.ORDER);
        m.put("order_delivered", Route.ORDER);
        m.put("order_returned", Route.ORDER);
        m.put("order_ready_to_process", Route.ORDER);
        m.put("order_ready_to_ship", Route.ORDER);
        m.put("order_deleted", Route.ORDER);
        m.put("order_resent", Route.ORDER);

        // --- handlers exist, but no Chargebee endpoint subscribes these yet ---
        m.put("invoice_created", Route.INVOICE);
        m.put("invoice_generated", Route.INVOICE);
        m.put("invoice_updated", Route.INVOICE);
        m.put("invoice_paid", Route.INVOICE);
        m.put("invoice_deleted", Route.INVOICE);
        m.put("invoice_voided", Route.INVOICE);
        m.put("payment_succeeded", Route.PAYMENT);
        m.put("payment_created", Route.PAYMENT);
        m.put("payment_updated", Route.PAYMENT);
        m.put("payment_failed", Route.PAYMENT);
        m.put("payment_deleted", Route.PAYMENT);

        ROUTES = Map.copyOf(m);
    }

    private WebhookEventCoverage() {
    }

    /** Route for an event type; {@link Route#UNHANDLED} when nothing consumes it. */
    public static Route routeFor(String eventType) {
        if (eventType == null) {
            return Route.UNHANDLED;
        }
        Route route = ROUTES.get(eventType.trim().toLowerCase(Locale.ROOT));
        return route == null ? Route.UNHANDLED : route;
    }

    /** True when some component consumes this event type. */
    public static boolean isCovered(String eventType) {
        return routeFor(eventType) != Route.UNHANDLED;
    }

    /** Every event type with a handler. */
    public static Set<String> coveredTypes() {
        return ROUTES.keySet();
    }
}
