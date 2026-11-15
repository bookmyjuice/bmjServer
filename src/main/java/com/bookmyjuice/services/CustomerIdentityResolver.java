package com.bookmyjuice.services;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.User;
import com.bookmyjuice.models.dto.CustomerIdentity;
import com.bookmyjuice.repository.CustomerRepository;
import com.bookmyjuice.repository.UserRepository;

/**
 * Single place that answers "which Chargebee customer belongs to this BMJ user?".
 *
 * <p>This resolver is read-only: it never writes the identity link (that is the
 * Identity Linker's job, see CHARGEBEE_SYNC_ARCHITECTURE.md §7). Its only purpose is to
 * stop a missing or dangling link from being silently flattened into a null key, which
 * then becomes an empty result set and a false "no subscription" screen.
 *
 * <p>Failure modes are reported, not swallowed: every non-resolved outcome is logged at
 * WARN/ERROR with the local user id, so the condition is visible in production logs even
 * before the reconciliation jobs of Phase 4 exist.
 */
@Service
public class CustomerIdentityResolver {

    private static final Logger logger = LoggerFactory.getLogger(CustomerIdentityResolver.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Resolve the identity of the authenticated principal.
     *
     * @param userDetails Spring Security principal, may be {@code null}
     * @return never {@code null}
     */
    public CustomerIdentity resolve(UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getId() == null) {
            logger.warn("Identity resolution rejected: no authenticated local user id");
            return CustomerIdentity.broken(null, "unauthenticated");
        }
        return resolveByLocalUserId(userDetails.getId());
    }

    /**
     * Resolve identity by BMJ user id. Exposed so jobs and tools can resolve a user
     * without a security context.
     */
    public CustomerIdentity resolveByLocalUserId(Long localUserId) {
        if (localUserId == null) {
            return CustomerIdentity.broken(null, "missing_local_user_id");
        }

        Optional<User> maybeUser = userRepository.findById(localUserId);
        if (maybeUser.isEmpty()) {
            logger.error("Identity resolution failed: no user row for local user {}", localUserId);
            return CustomerIdentity.broken(localUserId, "user_row_missing");
        }

        User user = maybeUser.get();
        String customerId = user.getChargebeeCustomerId();
        if (customerId == null || customerId.isBlank()) {
            logger.warn("Identity resolution: local user {} has no chargebee_customer_id", localUserId);
            return CustomerIdentity.unlinked(localUserId, "chargebee_customer_id_missing");
        }

        if (customerRepository.findById(customerId).isEmpty()) {
            logger.error("Identity resolution: local user {} points at missing customer row '{}'",
                    localUserId, customerId);
            return CustomerIdentity.broken(localUserId, "customer_row_missing:" + customerId);
        }

        return CustomerIdentity.resolved(customerId, localUserId);
    }

    /**
     * Convenience for callers that only need the key and already handle a null (for
     * example endpoints that must answer 401 when the identity cannot be established).
     */
    public String resolveCustomerIdOrNull(UserDetailsImpl userDetails) {
        CustomerIdentity identity = resolve(userDetails);
        return identity.isResolved() ? identity.getCustomerId() : null;
    }
}
