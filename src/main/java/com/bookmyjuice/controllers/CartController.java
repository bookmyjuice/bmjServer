package com.bookmyjuice.controllers;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.models.User;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.services.CartService;
import com.bookmyjuice.services.UserDetailsImpl;

/**
 * REST Controller for managing shopping cart operations.
 * Provides endpoints for adding items, viewing cart, and removing items.
 * Enforces "No Mixed Carts" rule - cannot mix one-time and subscription items.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /api/v1/cart - Get the current user's cart
     * Returns cart with items, subtotal, delivery fee, tax, and grand total.
     * Mobile app MUST use these server-calculated values, NOT calculate locally.
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getCart() {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            Map<String, Object> cartResponse = cartService.getCart(user);
            return ResponseEntity.ok(cartResponse);
        } catch (Exception e) {
            logger.error("Error fetching cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch cart: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/cart/items?priceId=xxx&quantity=2 - Add item to cart
     * Enforces "No Mixed Carts" rule - cannot mix 'charge' and 'plan' items.
     */
    @PostMapping("/items")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> addItem(
            @RequestParam String priceId,
            @RequestParam(defaultValue = "1") Integer quantity) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            if (priceId == null || priceId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "priceId is required"));
            }

            if (quantity < 1) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "quantity must be at least 1"));
            }

            Map<String, Object> cartResponse = cartService.addItem(user, priceId, quantity);
            return ResponseEntity.ok(cartResponse);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request - add item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.warn("Cart operation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error adding item to cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add item to cart: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/v1/cart/items/{priceId} - Remove item from cart by priceId
     */
    @DeleteMapping("/items/{priceId}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> removeItem(@PathVariable String priceId) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            if (priceId == null || priceId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "priceId is required"));
            }

            Map<String, Object> cartResponse = cartService.removeByPriceId(user, priceId);
            return ResponseEntity.ok(cartResponse);
        } catch (RuntimeException e) {
            logger.warn("Cart operation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error removing item from cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to remove item from cart: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/v1/cart/clear - Clear all items from the cart
     */
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> clearCart() {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            Map<String, Object> cartResponse = cartService.clearCart(user);
            return ResponseEntity.ok(cartResponse);
        } catch (Exception e) {
            logger.error("Error clearing cart: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear cart: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/cart/merge - Merge guest cart into authenticated cart

     * Handles conflicts when cart types differ with explicit user choice
     */
    @PostMapping("/merge")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> mergeCart(@RequestBody Map<String, Object> request) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String guestCartId = (String) request.get("guest_cart_id");
            String keepPreference = (String) request.get("keep");

            if (guestCartId == null || guestCartId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "guest_cart_id is required"));
            }

            Map<String, Object> mergeResult = cartService.mergeCarts(user, guestCartId, keepPreference);
            return ResponseEntity.ok(mergeResult);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid merge request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            logger.warn("Cart merge conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage(), "conflict", true));
        } catch (Exception e) {
            logger.error("Error merging carts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to merge carts: " + e.getMessage()));
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
