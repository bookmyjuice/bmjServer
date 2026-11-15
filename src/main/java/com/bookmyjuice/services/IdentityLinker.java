package com.bookmyjuice.services;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.User;
import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.repository.CustomerRepository;
import com.bookmyjuice.repository.UserRepository;

/**
 * The single writer of {@code users.chargebee_customer_id}.
 *
 * <p>Identity linking used to be a side effect of authentication: nine separate call sites
 * in {@code AuthController} and nothing at all on the webhook path. A customer created by
 * a hosted page therefore stayed unlinked forever, and every later read treated "unlinked"
 * as "no subscription". This service makes linking one explicit, reusable, testable
 * operation so that auth flows, webhooks and repair jobs all converge on the same rule.
 *
 * <p>Matching rule (CHARGEBEE_SYNC_ARCHITECTURE.md §3):
 * <ol>
 *   <li>phone first (raw, digits only, last ten digits), then email;</li>
 *   <li>exactly one match is required — two matches is a conflict, never a coin flip;</li>
 *   <li>a user already linked to a different customer is never silently re-pointed.</li>
 * </ol>
 */
@Service
public class IdentityLinker {

    private static final Logger logger = LoggerFactory.getLogger(IdentityLinker.class);

    /** What happened, so callers can log/alert instead of guessing. */
    public enum Outcome {
        /** The link was created by this call. */
        LINKED,
        /** The customer was already linked to a user; nothing changed. */
        ALREADY_LINKED,
        /** No BMJ user matches the customer's phone or email (legal: pre-signup customer). */
        NO_USER_MATCH,
        /** More than one BMJ user matches; deliberately not resolved. */
        AMBIGUOUS,
        /** A matching user is already linked to a different Chargebee customer. */
        CONFLICT,
        /** Malformed input or missing customer row. */
        REJECTED
    }

    /** Result of one linking attempt. */
    public static final class LinkResult {
        private final Outcome outcome;
        private final Long userId;
        private final String customerId;
        private final String detail;

        private LinkResult(Outcome outcome, Long userId, String customerId, String detail) {
            this.outcome = outcome;
            this.userId = userId;
            this.customerId = customerId;
            this.detail = detail;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public Long getUserId() {
            return userId;
        }

        public String getCustomerId() {
            return customerId;
        }

        public String getDetail() {
            return detail;
        }

        public boolean isLinked() {
            return outcome == Outcome.LINKED || outcome == Outcome.ALREADY_LINKED;
        }

        /** Test-only factory: lets unit tests build a result without a database. */
        static LinkResult forTesting(Outcome outcome, Long userId, String customerId) {
            return new LinkResult(outcome, userId, customerId, null);
        }

        @Override
        public String toString() {
            return "LinkResult{" + outcome + ", userId=" + userId + ", customerId=" + customerId
                    + (detail != null ? ", detail=" + detail : "") + "}";
        }
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Link a Chargebee customer to exactly one BMJ user, if the evidence is unambiguous.
     *
     * @param chargebeeCustomerId the Chargebee customer id (never a local user id)
     * @return the outcome; never {@code null}
     */
    @Transactional
    public LinkResult link(String chargebeeCustomerId) {
        if (chargebeeCustomerId == null || chargebeeCustomerId.isBlank()) {
            return new LinkResult(Outcome.REJECTED, null, null, "blank_customer_id");
        }

        CustomerEntity customer = customerRepository.findById(chargebeeCustomerId).orElse(null);
        if (customer == null) {
            logger.warn("Identity link rejected: no customer row for '{}'", chargebeeCustomerId);
            return new LinkResult(Outcome.REJECTED, null, chargebeeCustomerId, "customer_row_missing");
        }

        User alreadyLinked = findByChargebeeId(chargebeeCustomerId);
        if (alreadyLinked != null) {
            return new LinkResult(Outcome.ALREADY_LINKED, alreadyLinked.getId(), chargebeeCustomerId, null);
        }

        Match match = matchByPhone(customer.getPhone());
        if (match.ambiguous) {
            logger.error("Identity link ambiguous on phone '{}' for customer '{}'",
                    customer.getPhone(), chargebeeCustomerId);
            return new LinkResult(Outcome.AMBIGUOUS, null, chargebeeCustomerId, "multiple_users_match_phone");
        }
        if (match.user == null) {
            match = matchByEmail(customer.getEmail());
            if (match.ambiguous) {
                logger.error("Identity link ambiguous on email '{}' for customer '{}'",
                        customer.getEmail(), chargebeeCustomerId);
                return new LinkResult(Outcome.AMBIGUOUS, null, chargebeeCustomerId, "multiple_users_match_email");
            }
        }

        if (match.user == null) {
            logger.info("Identity link: no BMJ user matches customer '{}' (phone '{}', email '{}')",
                    chargebeeCustomerId, customer.getPhone(), customer.getEmail());
            return new LinkResult(Outcome.NO_USER_MATCH, null, chargebeeCustomerId, "no_user_match");
        }

        User user = match.user;
        String current = user.getChargebeeCustomerId();
        if (current != null && !current.isBlank()) {
            if (current.equals(chargebeeCustomerId)) {
                return new LinkResult(Outcome.ALREADY_LINKED, user.getId(), chargebeeCustomerId, null);
            }
            logger.error("Identity link conflict: user {} already linked to '{}', refusing '{}'",
                    user.getId(), current, chargebeeCustomerId);
            return new LinkResult(Outcome.CONFLICT, user.getId(), chargebeeCustomerId,
                    "user_linked_to:" + current);
        }

        user.setChargebeeCustomerId(chargebeeCustomerId);
        userRepository.save(user);
        logger.info("Identity link created: user {} -> customer '{}' (matched by {})",
                user.getId(), chargebeeCustomerId, match.matchedBy);
        return new LinkResult(Outcome.LINKED, user.getId(), chargebeeCustomerId, match.matchedBy);
    }

    private User findByChargebeeId(String chargebeeCustomerId) {
        try {
            return userRepository.findByChargebeeCustomerId(chargebeeCustomerId).orElse(null);
        } catch (IncorrectResultSizeDataAccessException e) {
            // Several users share one customer id: never throw from a read; the reconciler
            // reports and repairs it.
            logger.error("Several BMJ users point at customer '{}'", chargebeeCustomerId);
            return null;
        }
    }

    private Match matchByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Match.none();
        }
        for (String candidate : phoneCandidates(phone)) {
            Match match = lookupByUsernameAndPhone(candidate);
            if (match.user != null || match.ambiguous) {
                return match.withMatchedBy("phone");
            }
        }
        return Match.none();
    }

    private Match matchByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Match.none();
        }
        try {
            return new Match(userRepository.findByEmail(email.trim()).orElse(null), false, "email");
        } catch (IncorrectResultSizeDataAccessException e) {
            return new Match(null, true, "email");
        }
    }

    private Match lookupByUsernameAndPhone(String candidate) {
        try {
            User byUsername = userRepository.findByUsername(candidate).orElse(null);
            if (byUsername != null) {
                return new Match(byUsername, false, "phone");
            }
            return new Match(userRepository.findByPhone(candidate).orElse(null), false, "phone");
        } catch (IncorrectResultSizeDataAccessException e) {
            return new Match(null, true, "phone");
        }
    }

    /** Raw, digits-only and last-ten-digits forms, so formatted numbers still match. */
    private Set<String> phoneCandidates(String phone) {
        Set<String> candidates = new LinkedHashSet<>();
        String trimmed = phone.trim();
        candidates.add(trimmed);
        String digits = trimmed.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) {
            candidates.add(digits);
            if (digits.length() > 10) {
                candidates.add(digits.substring(digits.length() - 10));
            }
        }
        return candidates;
    }

    /** Internal carrier for one match attempt. */
    private static final class Match {
        private final User user;
        private final boolean ambiguous;
        private final String matchedBy;

        private Match(User user, boolean ambiguous, String matchedBy) {
            this.user = user;
            this.ambiguous = ambiguous;
            this.matchedBy = matchedBy;
        }

        static Match none() {
            return new Match(null, false, null);
        }

        Match withMatchedBy(String by) {
            return new Match(user, ambiguous, by);
        }
    }
}
