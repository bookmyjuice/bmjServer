package com.bookmyjuice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookmyjuice.services.SubscriptionService;
import com.bookmyjuice.services.UserDetailsImpl;
import com.chargebee.Result;
import com.chargebee.models.PortalSession;
import com.chargebee.models.PricingPageSession;

@Controller
@RequestMapping("api/test")
public class PricingPageController {

    private static final String pricingPageId = "01JR21FTRVHMMZ65H1M5HWPE92";
    // String pricingPageId = "01JR21FTRVHMMZ65H1M5HWPE92";
    @Autowired
    SubscriptionService subscriptionService;

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

    @GetMapping("/generate_pricing_page_session_url")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> pricingPage() {
        // System.err.println(req.getSubscriptionId());
        String customerId = getUserIdFromSecurityContext();
        if (customerId == null) {
            return ResponseEntity.badRequest().body("Customer ID not found");
        } else {
            if (subscriptionService.existsById(customerId)) {
                return generateExistingSubscriptionSession(subscriptionService.findSubscriptionByCustomerId(customerId));
            } else {
                return generateNewSubscriptionSession(customerId);
            }

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

    private ResponseEntity<?> generateExistingSubscriptionSession(String subscriptionId) {
        try {
            Result result = PricingPageSession.createForExistingSubscription()
                    .pricingPageId(pricingPageId)
                    .subscriptionId(subscriptionId)
                    // .customerId(req.getCustomerId())
                    .request();

            return ResponseEntity.ok(result.pricingPageSession().toJson());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ResponseEntity<?> generateNewSubscriptionSession(String customerId) {
        try {
            Result result = PricingPageSession.createForNewSubscription()
                    .pricingPageId(pricingPageId)
                    // .subscriptionId(req.getSubscriptionId())
                    .customerId(customerId)
                    .request();
            return ResponseEntity.ok(result.pricingPageSession().toJson());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
