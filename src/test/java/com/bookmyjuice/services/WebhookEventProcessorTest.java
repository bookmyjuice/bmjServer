package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Unit tests for WebhookEventProcessor
 *
 * TC-WEB-007: WebhookEventProcessor — last processing results (empty initially)
 */
class WebhookEventProcessorTest {

    private WebhookEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new WebhookEventProcessor();
    }

    @Test
    @DisplayName("TC-WEB-007: Get last processing results returns empty map")
    void testGetLastProcessingResults_Empty() {
        var results = processor.getLastProcessingResults();
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), "Results should be empty initially");
    }
}
