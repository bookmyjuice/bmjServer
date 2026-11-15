package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for {@link DriftDetectionService}.
 *
 * <p>The service is the read-only half of Phase 4: it must report drift accurately and must
 * never throw, because a detector failure may not take down the scheduler.
 *
 * <p>References: CHARGEBEE_RECONCILIATION_PLAN.md, CHARGEBEE_SYNC_ROADMAP.md Phase 4.
 */
class DriftDetectionServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DriftDetectionService service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("a clean projection reports no findings")
    void cleanReport() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);

        DriftDetectionService.DriftReport report = service.detect();

        assertTrue(report.isClean());
        assertEquals(0, report.getTotal());
    }

    @Test
    @DisplayName("each detector contributes to the total")
    void driftIsCounted() {
        when(jdbcTemplate.queryForObject(eq(DriftDetectionService.SQL_NULL_LINK_USERS), eq(Long.class)))
                .thenReturn(2L);
        when(jdbcTemplate.queryForObject(eq(DriftDetectionService.SQL_TWIN_KEY_ROWS), eq(Long.class)))
                .thenReturn(11L);
        when(jdbcTemplate.queryForObject(eq(DriftDetectionService.SQL_ORPHAN_SUBSCRIPTIONS), eq(Long.class)))
                .thenReturn(4L);
        when(jdbcTemplate.queryForObject(eq(DriftDetectionService.SQL_DANGLING_LINKS), eq(Long.class)))
                .thenReturn(0L);

        DriftDetectionService.DriftReport report = service.detect();

        assertEquals(2, report.getNullLinkUsers());
        assertEquals(11, report.getTwinKeyRows());
        assertEquals(4, report.getOrphanSubscriptions());
        assertEquals(0, report.getDanglingLinks());
        assertEquals(17, report.getTotal());
        assertFalse(report.isClean());
    }

    @Test
    @DisplayName("a failing detector is reported as -1 and does not throw")
    void detectorFailureIsContained() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new IllegalStateException("table missing"));

        DriftDetectionService.DriftReport report = service.detect();

        assertEquals(-1, report.getNullLinkUsers());
        // -1 is "could not check", so the run is not clean
        assertFalse(report.isClean());
    }
}
