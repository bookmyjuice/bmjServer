package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.bookmyjuice.models.User;
import com.bookmyjuice.models.dto.CustomerIdentity;
import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.repository.CustomerRepository;
import com.bookmyjuice.repository.UserRepository;

/**
 * Unit tests for {@link CustomerIdentityResolver}.
 *
 * <p>Guards the Phase 1 contract: an unresolved identity is reported as its own outcome
 * and can never be flattened into a null key (which used to become an empty list and a
 * false "no subscription" screen).
 *
 * <p>References: CHARGEBEE_SYNC_ARCHITECTURE.md §2, CHARGEBEE_SYNC_ROADMAP.md Phase 1.
 */
class CustomerIdentityResolverTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerIdentityResolver resolver;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    private User user(Long id, String chargebeeCustomerId) {
        User user = new User();
        user.setId(id);
        user.setChargebeeCustomerId(chargebeeCustomerId);
        return user;
    }

    private CustomerEntity customer(String id) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(id);
        return entity;
    }

    private UserDetailsImpl principal(Long id) {
        return new UserDetailsImpl(id, "9999999911", "qa@bookmyjuice.test", null, List.of());
    }

    @Test
    @DisplayName("resolved: returns the Chargebee customer id when the user is linked")
    void linkedUserResolves() {
        when(userRepository.findById(500L)).thenReturn(Optional.of(user(500L, "16CS3UVZQtF20nFJ")));
        when(customerRepository.findById("16CS3UVZQtF20nFJ"))
                .thenReturn(Optional.of(customer("16CS3UVZQtF20nFJ")));

        CustomerIdentity identity = resolver.resolve(principal(500L));

        assertTrue(identity.isResolved());
        assertEquals("16CS3UVZQtF20nFJ", identity.getCustomerId());
        assertEquals(500L, identity.getUserId());
        assertNull(identity.getDetail());
    }

    @Test
    @DisplayName("unlinked: null chargebee_customer_id, not an empty result")
    void nullLinkIsUnlinked() {
        when(userRepository.findById(500L)).thenReturn(Optional.of(user(500L, null)));

        CustomerIdentity identity = resolver.resolve(principal(500L));

        assertEquals(CustomerIdentity.Status.UNLINKED, identity.getStatus());
        assertNull(identity.getCustomerId());
        assertEquals("chargebee_customer_id_missing", identity.getDetail());
    }

    @Test
    @DisplayName("unlinked: blank chargebee_customer_id")
    void blankLinkIsUnlinked() {
        when(userRepository.findById(500L)).thenReturn(Optional.of(user(500L, "   ")));

        assertEquals(CustomerIdentity.Status.UNLINKED,
                resolver.resolve(principal(500L)).getStatus());
    }

    @Test
    @DisplayName("broken: missing user row")
    void missingUserIsBroken() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        CustomerIdentity identity = resolver.resolve(principal(999L));

        assertEquals(CustomerIdentity.Status.BROKEN, identity.getStatus());
        assertEquals("user_row_missing", identity.getDetail());
    }

    @Test
    @DisplayName("broken: link pointing at a missing customer row")
    void danglingLinkIsBroken() {
        when(userRepository.findById(502L)).thenReturn(Optional.of(user(502L, "169oAaVYQ0fXY5psP")));
        when(customerRepository.findById("169oAaVYQ0fXY5psP")).thenReturn(Optional.empty());

        CustomerIdentity identity = resolver.resolve(principal(502L));

        assertEquals(CustomerIdentity.Status.BROKEN, identity.getStatus());
        assertEquals("customer_row_missing:169oAaVYQ0fXY5psP", identity.getDetail());
    }

    @Test
    @DisplayName("broken: no principal, never a silent null")
    void noPrincipalIsBroken() {
        CustomerIdentity identity = resolver.resolve(null);

        assertEquals(CustomerIdentity.Status.BROKEN, identity.getStatus());
        assertEquals("unauthenticated", identity.getDetail());
    }

    @Test
    @DisplayName("compatibility accessor returns null for an unlinked user")
    void compatibilityAccessorReturnsNull() {
        when(userRepository.findById(500L)).thenReturn(Optional.of(user(500L, null)));

        assertNull(resolver.resolveCustomerIdOrNull(principal(500L)));
    }

    @Test
    @DisplayName("compatibility accessor returns the id when resolved")
    void compatibilityAccessorReturnsId() {
        when(userRepository.findById(500L)).thenReturn(Optional.of(user(500L, "cb_1")));
        when(customerRepository.findById("cb_1")).thenReturn(Optional.of(customer("cb_1")));

        assertEquals("cb_1", resolver.resolveCustomerIdOrNull(principal(500L)));
    }

    @Test
    @DisplayName("only RESOLVED counts as resolved")
    void onlyResolvedCountsAsResolved() {
        assertFalse(CustomerIdentity.unlinked(1L, "x").isResolved());
        assertFalse(CustomerIdentity.broken(1L, "y").isResolved());
        assertTrue(CustomerIdentity.resolved("cb", 1L).isResolved());
    }
}
