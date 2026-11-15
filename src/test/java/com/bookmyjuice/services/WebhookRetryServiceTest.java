package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.bookmyjuice.models.entities.WebhookEvent;
import com.bookmyjuice.repository.WebhookEventRepository;

/**
 * Unit tests for {@link WebhookRetryService}.
 *
 * <p>Before this worker existed the retry query and helper were dead code, so a failed
 * webhook was permanent. These tests pin the three outcomes that matter: success marks the
 * event completed, a transient failure marks it failed and keeps it queued, and an event
 * that exhausts {@link WebhookRetryService#MAX_ATTEMPTS} is dead-lettered.
 *
 * <p>References: CHARGEBEE_SYNC_ROADMAP.md Phase 3.
 */
class WebhookRetryServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private WebhookEventReplayer replayer;

    @InjectMocks
    private WebhookRetryService service;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    private WebhookEvent event(String id, int retryCount) {
        WebhookEvent event = new WebhookEvent();
        event.setEventId(id);
        event.setEventType("subscription_created");
        event.setProcessingStatus("FAILED");
        event.setRetryCount(retryCount);
        return event;
    }

    @Test
    @DisplayName("a successful replay marks the event completed")
    void successCompletes() throws Exception {
        when(webhookEventRepository.findEventsNeedingRetry()).thenReturn(List.of(event("evt_1", 1)));
        when(replayer.replay("evt_1", "subscription_created")).thenReturn(true);

        WebhookRetryService.RetryReport report = service.drainOnce();

        assertEquals(1, report.getRetried());
        assertEquals(1, report.getCompleted());
        assertEquals(0, report.getFailed());
        verify(idempotencyService).markEventCompleted("evt_1");
        verify(idempotencyService, never()).markEventDeadLettered(anyString(), anyString());
    }

    @Test
    @DisplayName("a failing replay stays queued while attempts remain")
    void failureStaysQueued() throws Exception {
        when(webhookEventRepository.findEventsNeedingRetry()).thenReturn(List.of(event("evt_2", 0)));
        when(replayer.replay("evt_2", "subscription_created")).thenReturn(false);

        WebhookRetryService.RetryReport report = service.drainOnce();

        assertEquals(1, report.getFailed());
        assertEquals(0, report.getDeadLettered());
        verify(idempotencyService).markEventFailed(eq("evt_2"), anyString());
        verify(idempotencyService, never()).markEventDeadLettered(anyString(), anyString());
    }

    @Test
    @DisplayName("the final attempt dead-letters the event")
    void exhaustedAttemptsAreDeadLettered() throws Exception {
        when(webhookEventRepository.findEventsNeedingRetry()).thenReturn(List.of(event("evt_3", 2)));
        when(replayer.replay("evt_3", "subscription_created")).thenReturn(false);

        WebhookRetryService.RetryReport report = service.drainOnce();

        assertEquals(1, report.getDeadLettered());
        verify(idempotencyService).markEventDeadLettered(eq("evt_3"), anyString());
    }

    @Test
    @DisplayName("an exception during replay is treated as a failure, not a crash")
    void exceptionIsContained() throws Exception {
        when(webhookEventRepository.findEventsNeedingRetry()).thenReturn(List.of(event("evt_4", 0)));
        when(replayer.replay("evt_4", "subscription_created")).thenThrow(new IllegalStateException("chargebee 502"));

        WebhookRetryService.RetryReport report = service.drainOnce();

        assertEquals(1, report.getRetried());
        assertEquals(1, report.getFailed());
        verify(idempotencyService).markEventFailed("evt_4", "chargebee 502");
    }

    @Test
    @DisplayName("an empty queue does nothing")
    void emptyQueue() {
        when(webhookEventRepository.findEventsNeedingRetry()).thenReturn(List.of());

        WebhookRetryService.RetryReport report = service.drainOnce();

        assertEquals(0, report.getRetried());
        verify(idempotencyService, never()).markEventCompleted(anyString());
    }

    @Test
    @DisplayName("a failing queue read is contained")
    void queueReadFailureIsContained() {
        when(webhookEventRepository.findEventsNeedingRetry())
                .thenThrow(new IllegalStateException("db down"));

        WebhookRetryService.RetryReport report = service.drainOnce();

        assertEquals(0, report.getRetried());
    }
}
