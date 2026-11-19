package com.bookmyjuice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the webhook payload guard.
 *
 * <p>Before this existed, a body without {@code id} produced a NullPointerException inside
 * the controller, which its catch-all turned into a 500 — and Chargebee retried a request
 * that could never succeed. Malformed input is now a 400.
 */
class WebhookPayloadValidationFilterTest {

    private static String reason(String body) {
        return WebhookPayloadValidationFilter.validate(body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("a valid body with an id passes")
    void validBodyPasses() {
        assertNull(reason("{\"id\":\"evt_123\",\"event_type\":\"subscription_created\"}"));
        assertNull(reason("{\"id\":\"evt_123\"}"));
    }

    @Test
    @DisplayName("empty body is rejected")
    void emptyBodyRejected() {
        assertNotNull(reason(""));
        assertEquals("empty request body", reason(""));
    }

    @Test
    @DisplayName("non-JSON body is rejected")
    void nonJsonRejected() {
        assertNotNull(reason("this is not json"));
        assertEquals("body is not valid JSON", reason("<xml/>"));
    }

    @Test
    @DisplayName("a JSON value that is not an object is rejected")
    void nonObjectRejected() {
        assertEquals("body must be a JSON object", reason("[1,2,3]"));
        assertEquals("body must be a JSON object", reason("\"just a string\""));
    }

    @Test
    @DisplayName("missing, null or blank id is rejected with a clear reason")
    void missingIdRejected() {
        assertEquals("missing event id", reason("{}"));
        assertEquals("missing event id", reason("{\"id\":null}"));
        assertEquals("missing event id", reason("{\"id\":\"   \"}"));
        assertEquals("missing event id", reason("{\"event_type\":\"subscription_created\"}"));
    }
}
