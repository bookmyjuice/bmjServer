package com.bookmyjuice.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEPRECATED — Returned 410 Gone.
 * 
 * Per enterprise architecture decision (ARCHITECTURE_OVERVIEW.md v3.0):
 * - All plan discovery, detail, comparison, and subscription management
 *   must use NATIVE BMJ screens (Flutter), NOT Chargebee-hosted pages.
 * - Chargebee Pricing Pages are REMOVED from user-facing flows.
 * - Only Chargebee Hosted Checkout is retained for final payment completion.
 * 
 * Replacement: Use BillingController for native billing orchestration
 * and SubscriptionController for native subscription management.
 * 
 * @deprecated Use native BMJ billing/subscription endpoints instead.
 *     See docs/NATIVE_BILLING_FLOW.md for the replacement architecture.
 *     Scheduled for removal in next major release.
 */
@Deprecated(since = "2026-05-08", forRemoval = true)
@RestController
@RequestMapping("api/test")
public class PricingPageController {

    private static final String GONE_MESSAGE = 
        "Chargebee Pricing Pages are no longer supported. " +
        "Use native BMJ endpoints for plan discovery and subscription management. " +
        "See docs/NATIVE_BILLING_FLOW.md for the replacement architecture. " +
        "Only Chargebee Hosted Checkout is retained for final payment completion.";

    @GetMapping("/generate_pricing_page_session_url")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> pricingPage() {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of(
                "error", "pricing_pages_deprecated",
                "message", GONE_MESSAGE,
                "documentation", "docs/NATIVE_BILLING_FLOW.md"
            ));
    }

    @PostMapping("/generate_plan_change_session_url")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> planChangePricingPage(@RequestBody String subscriptionId) {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of(
                "error", "pricing_pages_deprecated",
                "message", GONE_MESSAGE,
                "documentation", "docs/NATIVE_BILLING_FLOW.md"
            ));
    }

    @GetMapping("/generate_new_premium_subscription_pricing_page")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> generateNewPremiumSubscriptionPricingPage() {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of(
                "error", "pricing_pages_deprecated",
                "message", GONE_MESSAGE,
                "documentation", "docs/NATIVE_BILLING_FLOW.md"
            ));
    }

    @GetMapping("/generate_new_signature_subscription_pricing_page")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> generateNewSignatureSubscriptionPricingPage() {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of(
                "error", "pricing_pages_deprecated",
                "message", GONE_MESSAGE,
                "documentation", "docs/NATIVE_BILLING_FLOW.md"
            ));
    }

    @GetMapping("/generate_new_delight_subscription_pricing_page")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> generateNewDelightSubscriptionPricingPage() {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of(
                "error", "pricing_pages_deprecated",
                "message", GONE_MESSAGE,
                "documentation", "docs/NATIVE_BILLING_FLOW.md"
            ));
    }

    @GetMapping("/generate_existing_delight_subscription_session")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> generateExistingDelightSubscriptionSession() {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of(
                "error", "pricing_pages_deprecated",
                "message", GONE_MESSAGE,
                "documentation", "docs/NATIVE_BILLING_FLOW.md"
            ));
    }
}
