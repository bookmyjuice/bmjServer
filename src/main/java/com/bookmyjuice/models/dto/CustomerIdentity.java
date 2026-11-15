package com.bookmyjuice.models.dto;

/**
 * Outcome of resolving the Chargebee customer identity for an authenticated BMJ user.
 *
 * <p>Before this type existed the only way to ask "which customer is this user?" was a
 * nullable {@code String}, and a null was indistinguishable from "this customer has no
 * subscriptions" once it reached a repository query. That is what allowed the API to
 * answer {@code 200 {"subscriptions":[]}} for a user who does have a plan.
 *
 * <p>Three outcomes are now explicit:
 * <ul>
 *   <li>{@link Status#RESOLVED} — a usable Chargebee customer id was found.</li>
 *   <li>{@link Status#UNLINKED} — the local user has no {@code chargebee_customer_id}.
 *       Nothing was looked up; callers must not treat this as "no data".</li>
 *   <li>{@link Status#BROKEN} — the link exists but does not point at a usable customer
 *       row (or the user row itself is missing).</li>
 * </ul>
 *
 * <p>See CHARGEBEE_SYNC_ARCHITECTURE.md §2 (invariant I-1) and
 * CHARGEBEE_SYNC_ROADMAP.md Phase 1.
 */
public final class CustomerIdentity {

    /** Resolution outcome. */
    public enum Status {
        RESOLVED,
        UNLINKED,
        BROKEN
    }

    private final Status status;
    private final String customerId;
    private final Long userId;
    private final String detail;

    private CustomerIdentity(Status status, String customerId, Long userId, String detail) {
        this.status = status;
        this.customerId = customerId;
        this.userId = userId;
        this.detail = detail;
    }

    /** A usable Chargebee customer id was resolved. */
    public static CustomerIdentity resolved(String customerId, Long userId) {
        return new CustomerIdentity(Status.RESOLVED, customerId, userId, null);
    }

    /** The user exists but carries no Chargebee customer id. */
    public static CustomerIdentity unlinked(Long userId, String detail) {
        return new CustomerIdentity(Status.UNLINKED, null, userId, detail);
    }

    /** The link exists but is not usable, or the user row itself is missing. */
    public static CustomerIdentity broken(Long userId, String detail) {
        return new CustomerIdentity(Status.BROKEN, null, userId, detail);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isResolved() {
        return status == Status.RESOLVED;
    }

    /** Chargebee customer id; {@code null} unless {@link #isResolved()}. */
    public String getCustomerId() {
        return customerId;
    }

    /** BMJ user id when it was known at resolution time. */
    public Long getUserId() {
        return userId;
    }

    /** Machine-readable reason for a non-resolved outcome. */
    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return "CustomerIdentity{" + status + ", userId=" + userId
                + (customerId != null ? ", customerId=" + customerId : "")
                + (detail != null ? ", detail=" + detail : "") + "}";
    }
}
