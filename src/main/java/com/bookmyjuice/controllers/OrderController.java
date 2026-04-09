package com.bookmyjuice.controllers;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.models.entities.OrderEntity;
import com.bookmyjuice.services.OrderApiService;
import com.bookmyjuice.services.UserDetailsImpl;

/**
 * REST Controller for managing orders
 * Handles order retrieval and tracking
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderApiService orderApiService;

    /**
     * Get all orders for the current user
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getMyOrders() {
        try {
            String customerId = getCustomerIdFromSecurityContext();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            List<Map<String, Object>> orders = orderApiService.getCustomerOrders(customerId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", orders
            ));
        } catch (Exception e) {
            logger.error("Error fetching orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get specific order details
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        try {
            Map<String, Object> order = orderApiService.getOrderDetails(orderId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", order
            ));
        } catch (Exception e) {
            logger.error("Error fetching order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get local orders from database
     */
    @GetMapping("/local/history")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getLocalOrderHistory() {
        try {
            String customerId = getCustomerIdFromSecurityContext();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            List<OrderEntity> orders = orderApiService.getLocalCustomerOrders(customerId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", orders
            ));
        } catch (Exception e) {
            logger.error("Error fetching local orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get specific local order
     */
    @GetMapping("/local/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getLocalOrder(@PathVariable String orderId) {
        try {
            Optional<OrderEntity> order = orderApiService.getLocalOrder(orderId);

            if (order.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "data", order.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Order not found"));
            }
        } catch (Exception e) {
            logger.error("Error fetching local order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all orders (admin endpoint)
     */
    @GetMapping("/admin/all-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<OrderEntity> orders = orderApiService.getAllOrders();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", orders.size(),
                    "data", orders
            ));
        } catch (Exception e) {
            logger.error("Error fetching all orders: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
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
