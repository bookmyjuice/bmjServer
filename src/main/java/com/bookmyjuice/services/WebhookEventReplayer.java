package com.bookmyjuice.services;

/**
 * Re-runs the handler for a webhook event that was stored earlier.
 *
 * <p>Kept as an interface so the retry worker can be unit-tested without a live Chargebee
 * connection: the production implementation fetches the event by id and re-dispatches it,
 * while tests supply a fake.
 *
 * <p>References: CHARGEBEE_SYNC_ROADMAP.md Phase 3.
 */
public interface WebhookEventReplayer {

    /**
     * Replay one stored event.
     *
     * @param eventId   the Chargebee event id
     * @param eventType the stored event type, for routing and logging
     * @return {@code true} when the handler reported success
     * @throws Exception when the event could not be fetched or the handler threw
     */
    boolean replay(String eventId, String eventType) throws Exception;
}
