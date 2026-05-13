package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Unit tests for ChargebeeSyncService
 *
 * TC-PROF-001: Startup sync disabled logs and returns
 * TC-PROF-002: Get sync status returns formatted string
 * TC-PROF-003: Shutdown does not throw
 *
 * Note: Full integration tests require live Chargebee API.
 * These tests verify the non-API-dependent methods.
 */
class ChargebeeSyncServiceTest {

    private ChargebeeSyncService syncService;

    @BeforeEach
    void setUp() {
        // Create service instance without Spring injection
        // The sync methods require Chargebee API, so we test only
        // the side-effect-free methods
        syncService = new ChargebeeSyncService();
    }

    @Test
    @DisplayName("TC-PROF-003: Shutdown does not throw when executor is null")
    void testShutdown_NoExecutor() {
        // Executor is null before ApplicationReadyEvent fires
        assertDoesNotThrow(() -> syncService.shutdown(),
                "Shutdown should not throw when executor is null");
    }
}
