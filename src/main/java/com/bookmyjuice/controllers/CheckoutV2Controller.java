package com.bookmyjuice.controllers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.models.User;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.services.CheckoutService;
import com.bookmyjuice.services.UserDetailsImpl;

/**
 * REST Controller for one-time checkout operations (V2 - Cart-based).
 * Creates Chargebee Hosted Page URLs for one-time purchases.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v2/checkout")
public class CheckoutV2Controller {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutV2Controller.class);

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private UserRepository userRepository;

    /**
     * POST /api/v1/checkout - Create one-time checkout from user's cart
     * Returns a Chargebee Hosted Page URL for payment.
     * 
     * Requirements:
     * - User must be authenticated
     * - Cart must contain only 'charge' type items (no mixed carts)
     * - User must have a Chargebee customer ID
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> createCheckout() {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            Map<String, Object> response = checkoutService.checkoutOneTime(user);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Checkout session created successfully",
                    "data", response));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid checkout request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.warn("Checkout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating checkout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create checkout: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/checkout/with-items - Create one-time checkout with specific
     * items
     * Allows checkout without using the cart system.
     * 
     * Request body:
     * {
     * "items": [
     * { "price_id": "charge_xxx", "quantity": 2 },
     * { "price_id": "charge_yyy", "quantity": 1 }
     * ]
     * }
     */
    @PostMapping("/with-items")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> createCheckoutWithItems(@RequestBody Map<String, Object> request) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");

            if (items == null || items.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Items list is required and cannot be empty"));
            }

            Map<String, Object> response = checkoutService.checkoutOneTimeWithItems(user, items);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Checkout session created successfully",
                    "data", response));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid checkout request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.warn("Checkout failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating checkout: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create checkout: " + e.getMessage()));
        }
    }

    /**
     * Helper method to extract the authenticated User entity from the security
     * context.
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
