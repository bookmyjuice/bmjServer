package com.bookmyjuice.controllers;

import com.bookmyjuice.models.User;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.services.SubscriptionCheckoutService;
import com.bookmyjuice.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for subscription checkout operations.
 * Creates Chargebee Hosted Page URLs for subscription purchases with day-wise schedules.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/subscribe")
public class SubscribeController {

    private static final Logger logger = LoggerFactory.getLogger(SubscribeController.class);

    @Autowired
    private SubscriptionCheckoutService subscriptionCheckoutService;

    @Autowired
    private UserRepository userRepository;

    /**
     * POST /api/v1/subscribe - Create subscription checkout from user's cart
     * Returns a Chargebee Hosted Page URL for subscription payment.
     * 
     * Requirements:
     * - User must be authenticated
     * - Cart must contain only 'plan' type items (no mixed carts)
     * - User must have a Chargebee customer ID
     * 
     * Request body:
     * {
     *   "schedule": {
     *     "Monday": true,
     *     "Wednesday": true,
     *     "Friday": true
     *   }
     * }
     * 
     * The schedule object specifies which days of the week the delivery should occur.
     * Days with value 'true' indicate delivery on that day.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> createSubscription(@RequestBody Map<String, Object> request) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            // Extract schedule from request (optional)
            @SuppressWarnings("unchecked")
            Map<String, Boolean> schedule = (Map<String, Boolean>) request.get("schedule");

            Map<String, Object> response = subscriptionCheckoutService.checkoutSubscription(user, schedule);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Subscription checkout session created successfully",
                    "data", response));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid subscription request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.warn("Subscription checkout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating subscription checkout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create subscription checkout: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/subscribe/direct - Create subscription checkout directly with plan ID
     * Allows subscription without using the cart system.
     * 
     * Request body:
     * {
     *   "plan_id": "plan_delight_orange_weekly",
     *   "schedule": {
     *     "Monday": true,
     *     "Wednesday": true,
     *     "Friday": true
     *   }
     * }
     */
    @PostMapping("/direct")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> createSubscriptionDirect(@RequestBody Map<String, Object> request) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String planId = (String) request.get("plan_id");
            if (planId == null || planId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "plan_id is required"));
            }

            // Extract schedule from request (optional)
            @SuppressWarnings("unchecked")
            Map<String, Boolean> schedule = (Map<String, Boolean>) request.get("schedule");

            Map<String, Object> response = subscriptionCheckoutService.checkoutSubscriptionDirect(user, planId, schedule);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Subscription checkout session created successfully",
                    "data", response));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid subscription request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.warn("Subscription checkout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating subscription checkout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create subscription checkout: " + e.getMessage()));
        }
    }

    /**
     * Helper method to extract the authenticated User entity from the security context.
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }
}
