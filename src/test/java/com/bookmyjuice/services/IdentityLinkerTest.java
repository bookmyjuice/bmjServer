package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import com.bookmyjuice.models.User;
import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.repository.CustomerRepository;
import com.bookmyjuice.repository.UserRepository;

/**
 * Unit tests for {@link IdentityLinker} â€” the single writer of
 * {@code users.chargebee_customer_id}.
 *
 * <p>References: CHARGEBEE_SYNC_ARCHITECTURE.md Â§3, CHARGEBEE_SYNC_ROADMAP.md Phase 2.
 */
class IdentityLinkerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private IdentityLinker linker;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    private CustomerEntity customer(String id, String phone, String email) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(id);
        entity.setPhone(phone);
        entity.setEmail(email);
        return entity;
    }

    private User user(Long id, String chargebeeCustomerId) {
        User user = new User();
        user.setId(id);
        user.setUsername("9999999911");
        user.setChargebeeCustomerId(chargebeeCustomerId);
        return user;
    }

    @Test
    @DisplayName("links a user matched by phone")
    void linksByPhone() {
        when(customerRepository.findById("cb1")).thenReturn(Optional.of(customer("cb1", "9999999911", null)));
        when(userRepository.findByChargebeeCustomerId("cb1")).thenReturn(Optional.empty());
        User matched = user(500L, null);
        when(userRepository.findByUsername("9999999911")).thenReturn(Optional.of(matched));
        when(userRepository.save(any(User.class))).thenReturn(matched);

        IdentityLinker.LinkResult result = linker.link("cb1");

        assertEquals(IdentityLinker.Outcome.LINKED, result.getOutcome());
        assertEquals(500L, result.getUserId());
        assertEquals("cb1", matched.getChargebeeCustomerId());
        verify(userRepository).save(matched);
    }

    @Test
    @DisplayName("ignores formatting in the Chargebee phone number")
    void linksDespiteFormatting() {
        when(customerRepository.findById("cb1")).thenReturn(Optional.of(customer("cb1", "+1 (999) 999-9911", null)));
        when(userRepository.findByChargebeeCustomerId("cb1")).thenReturn(Optional.empty());
        User matched = user(500L, null);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByPhone("9999999911")).thenReturn(Optional.of(matched));
        when(userRepository.save(any(User.class))).thenReturn(matched);

        assertEquals(IdentityLinker.Outcome.LINKED, linker.link("cb1").getOutcome());
        assertEquals("cb1", matched.getChargebeeCustomerId());
    }

    @Test
    @DisplayName("falls back to email when the phone does not match")
    void linksByEmail() {
        when(customerRepository.findById("cb2")).thenReturn(Optional.of(customer("cb2", "5550001111", "qa@bookmyjuice.test")));
        when(userRepository.findByChargebeeCustomerId("cb2")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.empty());
        User matched = user(600L, null);
        when(userRepository.findByEmail("qa@bookmyjuice.test")).thenReturn(Optional.of(matched));
        when(userRepository.save(any(User.class))).thenReturn(matched);

        IdentityLinker.LinkResult result = linker.link("cb2");

        assertEquals(IdentityLinker.Outcome.LINKED, result.getOutcome());
        assertEquals("email", result.getDetail());
    }

    @Test
    @DisplayName("already linked is a no-op")
    void alreadyLinked() {
        when(customerRepository.findById("cb1")).thenReturn(Optional.of(customer("cb1", "9999999911", null)));
        when(userRepository.findByChargebeeCustomerId("cb1")).thenReturn(Optional.of(user(500L, "cb1")));

        IdentityLinker.LinkResult result = linker.link("cb1");

        assertEquals(IdentityLinker.Outcome.ALREADY_LINKED, result.getOutcome());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("no matching user is reported, not forced")
    void noUserMatch() {
        when(customerRepository.findById("cb9")).thenReturn(Optional.of(customer("cb9", "5550001111", "nobody@x.test")));
        when(userRepository.findByChargebeeCustomerId("cb9")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByPhone(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        IdentityLinker.LinkResult result = linker.link("cb9");

        assertEquals(IdentityLinker.Outcome.NO_USER_MATCH, result.getOutcome());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("a user already linked elsewhere is a conflict, never re-pointed")
    void conflictIsNotResolved() {
        when(customerRepository.findById("cb2")).thenReturn(Optional.of(customer("cb2", "9999999911", null)));
        when(userRepository.findByChargebeeCustomerId("cb2")).thenReturn(Optional.empty());
        User matched = user(500L, "cb_other");
        when(userRepository.findByUsername("9999999911")).thenReturn(Optional.of(matched));

        IdentityLinker.LinkResult result = linker.link("cb2");

        assertEquals(IdentityLinker.Outcome.CONFLICT, result.getOutcome());
        assertEquals("cb_other", matched.getChargebeeCustomerId());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("two users matching the same phone is ambiguous, never a coin flip")
    void ambiguousIsNotResolved() {
        when(customerRepository.findById("cb3")).thenReturn(Optional.of(customer("cb3", "9999999911", null)));
        when(userRepository.findByChargebeeCustomerId("cb3")).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString()))
                .thenThrow(new IncorrectResultSizeDataAccessException(2));

        IdentityLinker.LinkResult result = linker.link("cb3");

        assertEquals(IdentityLinker.Outcome.AMBIGUOUS, result.getOutcome());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("blank customer id is rejected")
    void blankIdRejected() {
        assertEquals(IdentityLinker.Outcome.REJECTED, linker.link("  ").getOutcome());
        assertEquals(IdentityLinker.Outcome.REJECTED, linker.link(null).getOutcome());
    }

    @Test
    @DisplayName("missing customer row is rejected")
    void missingCustomerRejected() {
        when(customerRepository.findById("cb_missing")).thenReturn(Optional.empty());

        IdentityLinker.LinkResult result = linker.link("cb_missing");

        assertEquals(IdentityLinker.Outcome.REJECTED, result.getOutcome());
        assertEquals("customer_row_missing", result.getDetail());
    }

    @Test
    @DisplayName("isLinked is false for a rejected attempt")
    void isLinkedSemantics() {
        assertFalse(linker.link(" ").isLinked());
    }

    // ---------------------------------------------------------------- linkUser (self-heal)

    @Test
    @DisplayName("self-heal: a valid existing link is left untouched")
    void linkUserAlreadyLinked() {
        when(userRepository.findById(500L)).thenReturn(Optional.of(user(500L, "cb1")));
        when(customerRepository.findById("cb1")).thenReturn(Optional.of(customer("cb1", null, null)));

        IdentityLinker.LinkResult result = linker.linkUser(500L);

        assertEquals(IdentityLinker.Outcome.ALREADY_LINKED, result.getOutcome());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("self-heal: a missing link is derived from the customer mirror by phone")
    void linkUserDerivesFromPhone() {
        User u = user(500L, null);
        when(userRepository.findById(500L)).thenReturn(Optional.of(u));
        when(customerRepository.findByPhone("9999999911"))
                .thenReturn(List.of(customer("cb1", "9999999911", null)));
        when(userRepository.save(any(User.class))).thenReturn(u);

        IdentityLinker.LinkResult result = linker.linkUser(500L);

        assertEquals(IdentityLinker.Outcome.LINKED, result.getOutcome());
        assertEquals("cb1", u.getChargebeeCustomerId());
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("self-heal: a dangling link is re-derived")
    void linkUserRepairsDanglingLink() {
        User u = user(504L, "cb_gone");
        when(userRepository.findById(504L)).thenReturn(Optional.of(u));
        when(customerRepository.findById("cb_gone")).thenReturn(Optional.empty());
        when(customerRepository.findByPhone("9999999911"))
                .thenReturn(List.of(customer("cb_real", "9999999911", null)));
        when(userRepository.save(any(User.class))).thenReturn(u);

        IdentityLinker.LinkResult result = linker.linkUser(504L);

        assertEquals(IdentityLinker.Outcome.LINKED, result.getOutcome());
        assertEquals("cb_real", u.getChargebeeCustomerId());
    }

    @Test
    @DisplayName("self-heal: with numeric and Chargebee twins, the Chargebee id wins")
    void linkUserPrefersChargebeeShapedId() {
        User u = user(504L, null);
        when(userRepository.findById(504L)).thenReturn(Optional.of(u));
        when(customerRepository.findByPhone("9999999911")).thenReturn(List.of(
                customer("504", "9999999911", null),
                customer("16CS3UVZQtF20nFJ", "9999999911", null)));
        when(userRepository.save(any(User.class))).thenReturn(u);

        IdentityLinker.LinkResult result = linker.linkUser(504L);

        assertEquals(IdentityLinker.Outcome.LINKED, result.getOutcome());
        assertEquals("16CS3UVZQtF20nFJ", u.getChargebeeCustomerId());
    }

    @Test
    @DisplayName("self-heal: no matching customer is reported, nothing is written")
    void linkUserNoMatch() {
        when(userRepository.findById(500L)).thenReturn(Optional.of(user(500L, null)));
        when(customerRepository.findByPhone(anyString())).thenReturn(List.of());
        when(customerRepository.findByEmailIgnoreCase(anyString())).thenReturn(List.of());

        IdentityLinker.LinkResult result = linker.linkUser(500L);

        assertEquals(IdentityLinker.Outcome.NO_USER_MATCH, result.getOutcome());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("self-heal: two indistinct customers is ambiguous, nothing is written")
    void linkUserAmbiguous() {
        when(userRepository.findById(500L)).thenReturn(Optional.of(user(500L, null)));
        when(customerRepository.findByPhone("9999999911")).thenReturn(List.of(
                customer("100", "9999999911", null),
                customer("200", "9999999911", null)));

        IdentityLinker.LinkResult result = linker.linkUser(500L);

        assertEquals(IdentityLinker.Outcome.AMBIGUOUS, result.getOutcome());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("self-heal: an unknown user is rejected")
    void linkUserUnknownUser() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertEquals(IdentityLinker.Outcome.REJECTED, linker.linkUser(999L).getOutcome());
    }

    @Test
    @DisplayName("self-heal is idempotent: a second call changes nothing")
    void linkUserIsIdempotent() {
        User u = user(500L, null);
        when(userRepository.findById(500L)).thenReturn(Optional.of(u));
        when(customerRepository.findByPhone("9999999911"))
                .thenReturn(List.of(customer("cb1", "9999999911", null)));
        when(userRepository.save(any(User.class))).thenReturn(u);

        assertEquals(IdentityLinker.Outcome.LINKED, linker.linkUser(500L).getOutcome());

        // second call: the user now carries the link and the customer row exists
        when(customerRepository.findById("cb1")).thenReturn(Optional.of(customer("cb1", "9999999911", null)));
        assertEquals(IdentityLinker.Outcome.ALREADY_LINKED, linker.linkUser(500L).getOutcome());
        verify(userRepository, times(1)).save(any(User.class));
    }
}

