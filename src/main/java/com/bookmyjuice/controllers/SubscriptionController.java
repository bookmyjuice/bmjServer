package com.bookmyjuice.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.models.entities.SubscriptionEntity;
import com.bookmyjuice.repository.CustomerRepository;
import com.bookmyjuice.repository.SubscriptionEntityRepository;
import com.bookmyjuice.services.SubscriptionApiService;
import com.bookmyjuice.services.UserDetailsImpl;
import com.chargebee.models.HostedPage;

/**
 * REST Controller for managing subscriptions
 * Handles subscription creation, listing, pausing, resuming, and canceling
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    @Autowired
    private SubscriptionApiService subscriptionApiService;

    @Autowired
    private SubscriptionEntityRepository subscriptionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Get all subscriptions for the current user
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getMySubscriptions() {
        try {
            String customerId = getCustomerIdFromSecurityContext();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            Optional<CustomerEntity> customer = customerRepository.findById(customerId);
            List<SubscriptionEntity> subscriptions = customer.isPresent()
                    ? subscriptionRepository.findByCustomer(customer.get())
                    : List.of();

            if (subscriptions.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "No subscriptions found",
                        "subscriptions", subscriptions));
            }

            List<Map<String, Object>> subscriptionData = subscriptions.stream()
                    .map(this::mapSubscriptionToResponse)
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Subscriptions retrieved successfully",
                    "subscriptions", subscriptionData));
        } catch (Exception e) {
            logger.error("Error fetching subscriptions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch subscriptions"));
        }
    }

    /**
     * Get specific subscription details
     */
    @GetMapping("/{subscriptionId}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getSubscription(@PathVariable String subscriptionId) {
        try {
            Map<String, Object> subscription = subscriptionApiService.getSubscriptionDetails(subscriptionId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", subscription));
        } catch (Exception e) {
            logger.error("Error fetching subscription: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create a new subscription (returns hosted page URL)
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> createSubscription(@RequestBody Map<String, String> request) {
        try {
            String customerId = getCustomerIdFromSecurityContext();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String planId = request.get("planId");
            if (planId == null || planId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "planId is required"));
            }

            HostedPage hostedPage = subscriptionApiService.createSubscriptionHostedPage(customerId, planId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Hosted page created successfully",
                    "hostedPageId", hostedPage.id(),
                    "url", hostedPage.url()));
        } catch (Exception e) {
            logger.error("Error creating subscription: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Pause a subscription
     */
    @PutMapping("/{subscriptionId}/pause")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> pauseSubscription(@PathVariable String subscriptionId) {
        try {
            boolean success = subscriptionApiService.pauseSubscription(subscriptionId);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Subscription paused successfully",
                        "subscriptionId", subscriptionId));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Failed to pause subscription"));
            }
        } catch (Exception e) {
            logger.error("Error pausing subscription {}: {}", subscriptionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to pause subscription: " + e.getMessage()));
        }
    }

    /**
     * Resume a paused subscription
     */
    @PutMapping("/{subscriptionId}/resume")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> resumeSubscription(@PathVariable String subscriptionId) {
        try {
            boolean success = subscriptionApiService.resumeSubscription(subscriptionId);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Subscription resumed successfully",
                        "subscriptionId", subscriptionId));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Failed to resume subscription"));
            }
        } catch (Exception e) {
            logger.error("Error resuming subscription {}: {}", subscriptionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to resume subscription: " + e.getMessage()));
        }
    }

    /**
     * Cancel a subscription
     */
    @DeleteMapping("/{subscriptionId}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> cancelSubscription(@PathVariable String subscriptionId) {
        try {
            boolean success = subscriptionApiService.cancelSubscription(subscriptionId);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Subscription canceled successfully",
                        "subscriptionId", subscriptionId));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Failed to cancel subscription"));
            }
        } catch (Exception e) {
            logger.error("Error canceling subscription {}: {}", subscriptionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to cancel subscription: " + e.getMessage()));
        }
    }

    /**
     * Get all available subscription plans
     */
    @GetMapping("/pricing/plans")
    public ResponseEntity<?> getAllPlans() {
        try {
            List<Map<String, Object>> plans = subscriptionApiService.getAllPlans();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", plans));
        } catch (Exception e) {
            logger.error("Error fetching plans: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get pricing page for customer
     */
    @GetMapping("/pricing-page")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getPricingPage() {
        try {
            String customerId = getCustomerIdFromSecurityContext();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String pricingPageUrl = subscriptionApiService.getPricingPageUrl(customerId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Pricing page retrieved",
                    "url", pricingPageUrl));
        } catch (Exception e) {
            logger.error("Error getting pricing page: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Helper method to map subscription entity to response DTO
     */
    private Map<String, Object> mapSubscriptionToResponse(SubscriptionEntity subscription) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", subscription.getId());
        map.put("customerId", subscription.getCustomer() != null ? subscription.getCustomer().getId() : "");
        map.put("planId", subscription.getPlanId() != null ? subscription.getPlanId() : "");
        map.put("status", subscription.getStatus() != null ? subscription.getStatus() : "active");
        map.put("billingPeriod", subscription.getBillingPeriod());
        map.put("billingPeriodUnit",
                subscription.getBillingPeriodUnit() != null ? subscription.getBillingPeriodUnit() : "month");
        map.put("currentTermStart", subscription.getCurrentTermStart());
        map.put("currentTermEnd", subscription.getCurrentTermEnd());
        map.put("nextBillingAt", subscription.getNextBillingAt());
        map.put("createdAt", subscription.getCreatedAt());
        map.put("updatedAt", subscription.getUpdatedAt());
        map.put("items", subscription.getItems());
        // map.put("status", subscription.getStatus());
        return map;
    }

    /**
     * Extract customer ID from security context
     */
    private String getCustomerIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId().toString();
        }
        return null;
    }
}
