package com.bookmyjuice.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEPRECATED — Returned 410 Gone.
 * 
 * Per enterprise architecture decision (ARCHITECTURE_OVERVIEW.md v3.0):
 * - All subscription management must use NATIVE BMJ screens (Flutter),
 *   NOT Chargebee Portal (Self-Serve Page).
 * - Native SubscriptionManagementScreen replaces this functionality.
 * 
 * Replacement: Use SubscriptionController for native subscription management.
 * 
 * @deprecated Use native BMJ subscription management endpoints instead.
 *     See docs/NATIVE_BILLING_FLOW.md for the replacement architecture.
 *     Scheduled for removal in next major release.
 */
@Deprecated(since = "2026-05-08", forRemoval = true)
@RestController
@RequestMapping("api/test")
public class SelfServePageController {

    private static final String GONE_MESSAGE =
        "Chargebee Portal (Self-Serve Page) is no longer supported. " +
        "Use native BMJ subscription management endpoints instead. " +
        "See SubscriptionController and docs/NATIVE_BILLING_FLOW.md for the replacement architecture.";

    @GetMapping("/portal")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> portal() {
        return ResponseEntity.status(HttpStatus.GONE)
            .body(Map.of(
                "error", "portal_deprecated",
                "message", GONE_MESSAGE,
                "documentation", "docs/NATIVE_BILLING_FLOW.md"
            ));
    }
}
