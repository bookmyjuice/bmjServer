package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link WebhookIdentityHook}.
 *
 * <p>The contract is narrow and deliberate: link when a customer id is present, do nothing
 * when it is absent, and never let a linking failure escape into the webhook response.
 *
 * <p>References: CHARGEBEE_SYNC_ROADMAP.md Phase 2.
 */
class WebhookIdentityHookTest {

    @Mock
    private IdentityLinker identityLinker;

    @InjectMocks
    private WebhookIdentityHook hook;

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
    @DisplayName("links the customer carried by the event")
    void linksCustomer() {
        when(identityLinker.link("cb1"))
                .thenReturn(newLinkResult(IdentityLinker.Outcome.LINKED, "cb1"));

        hook.linkForCustomer("cb1", "evt_1");

        verify(identityLinker).link("cb1");
    }

    @Test
    @DisplayName("no customer id means no attempt")
    void skipsWhenNoCustomer() {
        hook.linkForCustomer(null, "evt_2");
        hook.linkForCustomer("   ", "evt_2");

        verify(identityLinker, never()).link(anyString());
    }

    @Test
    @DisplayName("a linking failure never escapes the hook")
    void swallowsFailures() {
        when(identityLinker.link("cb2")).thenThrow(new IllegalStateException("db down"));

        assertDoesNotThrow(() -> hook.linkForCustomer("cb2", "evt_3"));
    }

    @Test
    @DisplayName("a null event is ignored")
    void nullEventIgnored() {
        assertDoesNotThrow(() -> hook.afterMirroring(null));
        verify(identityLinker, never()).link(anyString());
    }

    /** Builds a LinkResult through the public factory path (outcome + ids only). */
    private IdentityLinker.LinkResult newLinkResult(IdentityLinker.Outcome outcome, String customerId) {
        IdentityLinker.LinkResult result = IdentityLinker.LinkResult.forTesting(outcome, 1L, customerId);
        return result;
    }
}
