package com.bookmyjuice.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Unit tests for RateLimiterService
 *
 * TC-SEC-001: Rate limiter allows requests under limit
 * TC-SEC-002: Rate limiter returns non-null remaining tokens
 * TC-SEC-003: Rate limiter handles null key gracefully
 * TC-SEC-004: Rate limiter handles empty key
 */
class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    @DisplayName("TC-SEC-001: Rate limiter allows requests under limit")
    void testIsAllowed_UnderLimit() {
        String key = "test-key";
        assertTrue(rateLimiterService.isAllowed(key));
    }

    @Test
    @DisplayName("TC-SEC-002: Rate limiter returns non-null remaining tokens")
    void testGetRemainingTokens() {
        String key = "test-key-2";
        rateLimiterService.isAllowed(key);
        long remaining = rateLimiterService.getRemainingTokens(key);
        assertTrue(remaining >= 0, "Remaining tokens should be non-negative");
    }

    @Test
    @DisplayName("TC-SEC-003: Rate limiter handles null key gracefully")
    void testIsAllowed_NullKey() {
        assertDoesNotThrow(() -> rateLimiterService.isAllowed(null));
    }

    @Test
    @DisplayName("TC-SEC-004: Rate limiter handles empty key")
    void testIsAllowed_EmptyKey() {
        assertDoesNotThrow(() -> rateLimiterService.isAllowed(""));
    }

    @Test
    @DisplayName("TC-SEC-005: Global auth limit is allowed initially")
    void testGlobalAuthLimit_Allowed() {
        assertTrue(rateLimiterService.isGlobalAuthLimitAllowed());
    }

    @Test
    @DisplayName("TC-SEC-006: Reset limit does not throw")
    void testResetLimit() {
        assertDoesNotThrow(() -> rateLimiterService.resetLimit("some-ip"));
    }

    @Test
    @DisplayName("TC-SEC-007: Clear all limits does not throw")
    void testClearAllLimits() {
        assertDoesNotThrow(() -> rateLimiterService.clearAllLimits());
    }
}
