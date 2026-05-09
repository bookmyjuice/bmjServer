package com.bookmyjuice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookmyjuice.services.UserDetailsImpl;
import com.chargebee.Result;
import com.chargebee.models.PortalSession;

/**
 * DEPRECATED: Generates Chargebee Portal Session URLs for self-service
 * subscription management.
 * 
 * Per enterprise architecture decision (ARCHITECTURE_OVERVIEW.md v3.0):
 * - All subscription management must use NATIVE BMJ screens (Flutter),
 *   NOT Chargebee Portal (Self-Serve Page).
 * - Native SubscriptionManagementScreen replaces this functionality.
 * 
 * Replacement: Use SubscriptionController for native subscription management.
 * 
 * @deprecated Use native BMJ subscription management endpoints instead.
 *     Scheduled for removal in next major release.
 *     See docs/NATIVE_BILLING_FLOW.md for the replacement architecture.
 */
@Deprecated(since = "2026-05-08", forRemoval = true)
@Controller
@RequestMapping("api/test")
public class SelfServePageController {

    @GetMapping("/portal")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> postMethodName() {
        try {
            Result result = PortalSession.create()
                    
                    .customerId(getUserIdFromSecurityContext())
                    .request();
            PortalSession portalSession = result.portalSession();
            // System.err.println(portalSession.toString());
            return ResponseEntity.ok(portalSession.toJson());
   } catch (Exception e) {
            return ResponseEntity.ok(e.getMessage());
        }
    }


    public String getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId().toString();
        }
        return null; // Or throw an exception
    }

}
