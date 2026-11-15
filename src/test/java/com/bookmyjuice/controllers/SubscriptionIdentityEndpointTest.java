package com.bookmyjuice.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bookmyjuice.models.dto.CustomerIdentity;
import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.models.entities.SubscriptionEntity;
import com.bookmyjuice.repository.CustomerRepository;
import com.bookmyjuice.repository.SubscriptionEntityRepository;
import com.bookmyjuice.services.CustomerIdentityResolver;
import com.bookmyjuice.services.UserDetailsImpl;

/**
 * Phase 1 contract for {@code GET /api/subscriptions/my}:
 * an unresolved identity must never be answered with {@code 200 {"subscriptions":[]}}.
 *
 * <p>References: CHARGEBEE_SYNC_ARCHITECTURE.md §2, CHARGEBEE_SYNC_ROADMAP.md Phase 1.
 */
class SubscriptionIdentityEndpointTest {

    @Mock
    private CustomerIdentityResolver identityResolver;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SubscriptionEntityRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionController controller;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        UserDetailsImpl principal =
                new UserDetailsImpl(504L, "9876543210", "qa@bookmyjuice.test", null, List.of());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        mocks.close();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<?> response) {
        return (Map<String, Object>) response.getBody();
    }

    @Test
    @DisplayName("unlinked identity -> 409 identity_unlinked, never 200 []")
    void unlinkedIsConflict() {
        when(identityResolver.resolve(any()))
                .thenReturn(CustomerIdentity.unlinked(504L, "chargebee_customer_id_missing"));

        ResponseEntity<?> response = controller.getMySubscriptions();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("identity_unlinked", body(response).get("error"));
        assertEquals("chargebee_customer_id_missing", body(response).get("detail"));
    }

    @Test
    @DisplayName("broken identity -> 409 identity_broken")
    void brokenIsConflict() {
        when(identityResolver.resolve(any()))
                .thenReturn(CustomerIdentity.broken(504L, "customer_row_missing:cb_x"));

        ResponseEntity<?> response = controller.getMySubscriptions();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("identity_broken", body(response).get("error"));
    }

    @Test
    @DisplayName("resolved but customer row vanished -> 409 identity_broken")
    void resolvedWithMissingRowIsConflict() {
        when(identityResolver.resolve(any()))
                .thenReturn(CustomerIdentity.resolved("cb_x", 504L));
        when(customerRepository.findById("cb_x")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getMySubscriptions();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("identity_broken", body(response).get("error"));
    }

    @Test
    @DisplayName("rows under another customer key -> 409 subscription_unavailable (not a false empty)")
    void identityMismatchIsReported() {
        when(identityResolver.resolve(any()))
                .thenReturn(CustomerIdentity.resolved("16CS3UVZQtF20nFJ", 504L));
        CustomerEntity customer = new CustomerEntity();
        customer.setId("16CS3UVZQtF20nFJ");
        when(customerRepository.findById("16CS3UVZQtF20nFJ")).thenReturn(Optional.of(customer));
        when(subscriptionRepository.findByCustomerGuarded(customer)).thenReturn(List.of());
        when(subscriptionRepository.findByCustomerIdValue("504"))
                .thenReturn(List.of(new SubscriptionEntity()));

        ResponseEntity<?> response = controller.getMySubscriptions();

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("subscription_unavailable", body(response).get("error"));
        assertEquals("identity_mismatch", body(response).get("reason"));
    }

    @Test
    @DisplayName("genuinely no subscriptions -> 200 with an empty list (still legitimate)")
    void noRowsAnywhereIsOk() {
        when(identityResolver.resolve(any()))
                .thenReturn(CustomerIdentity.resolved("cb_x", 504L));
        CustomerEntity customer = new CustomerEntity();
        customer.setId("cb_x");
        when(customerRepository.findById("cb_x")).thenReturn(Optional.of(customer));
        when(subscriptionRepository.findByCustomerGuarded(customer)).thenReturn(List.of());
        when(subscriptionRepository.findByCustomerIdValue("504")).thenReturn(List.of());

        ResponseEntity<?> response = controller.getMySubscriptions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("success", body(response).get("status"));
        assertInstanceOf(List.class, body(response).get("subscriptions"));
    }
}
