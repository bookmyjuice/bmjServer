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

import com.bookmyjuice.models.User;
import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.models.entities.OrderEntity;
import com.bookmyjuice.repository.CustomerRepository;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.services.OrderApiService;
import com.bookmyjuice.services.OrderService;
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

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Production replacement for the non-production
     * {@code GET /api/test/ordersByCustomerId} route. (The {@code OrdersController}
     * that carried it was deleted on 2026-10-16 — blocker {@code B2}.)
     *
     * Returns the same raw JSON array produced by
     * {@link OrderService#fetchOrdersByCustomerId(String)}; the Flutter client
     * contract is unchanged.
     *
     * <p>D-6 fix: the lookup key is now the user's <em>Chargebee</em> customer id
     * (see {@link #resolveChargebeeCustomerId(Long)}), because
     * {@code ORDER_ENTITY.customer_id} stores the Chargebee id written by
     * {@code OrderMapper.toEntity} &mdash; the local numeric id could never match,
     * so this route always returned {@code []}. The retired
     * {@code /api/test/ordersByCustomerId} route had the identical lookup defect.</p>
     */
    @GetMapping("/by-customer")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getOrdersByCustomerId() {
        try {
            UserDetailsImpl principal = currentUser();
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String chargebeeCustomerId = resolveChargebeeCustomerId(principal.getId());
            if (chargebeeCustomerId == null) {
                // Authenticated, but the account has no Chargebee customer yet. No order row
                // can match, so an empty array preserves the pre-D-6 response shape.
                logger.warn("No Chargebee customer id for user {}; returning an empty order list",
                        principal.getId());
                return ResponseEntity.ok(List.of());
            }

            return ResponseEntity.ok(orderService.fetchOrdersByCustomerId(chargebeeCustomerId));
        } catch (Exception e) {
            logger.error("Error fetching orders by customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all orders for the current user
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getMyOrders() {
        try {
            UserDetailsImpl principal = currentUser();
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String chargebeeCustomerId = resolveChargebeeCustomerId(principal.getId());
            if (chargebeeCustomerId == null) {
                logger.warn("No Chargebee customer id for user {}; returning an empty order list",
                        principal.getId());
                return ResponseEntity.ok(Map.of("status", "success", "data", List.of()));
            }

            List<Map<String, Object>> orders = orderApiService.getCustomerOrders(chargebeeCustomerId);

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
            UserDetailsImpl principal = currentUser();
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String chargebeeCustomerId = resolveChargebeeCustomerId(principal.getId());
            if (chargebeeCustomerId == null) {
                logger.warn("No Chargebee customer id for user {}; returning an empty order list",
                        principal.getId());
                return ResponseEntity.ok(Map.of("status", "success", "data", List.of()));
            }

            List<OrderEntity> orders = orderApiService.getLocalCustomerOrders(chargebeeCustomerId);

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
     * The authenticated principal, or {@code null} when no resolvable user is bound
     * to the request ({@code @PreAuthorize} normally prevents this).
     */
    private UserDetailsImpl currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Resolve a local user id to the Chargebee customer id used by
     * {@code ORDER_ENTITY.customer_id} and by the Chargebee order APIs.
     *
     * <p>Mirrors {@code SubscriptionController#getCustomerIdFromSecurityContext()},
     * the canonical pattern in this codebase: {@code userRepository.findById(localId)}
     * &rarr; {@code user.getChargebeeCustomerId()}, with a legacy
     * {@link CustomerRepository} fallback for rows keyed by the local id.</p>
     *
     * @param localUserId local {@code USER_ENTITY.id}
     * @return the Chargebee customer id, or {@code null} when none is resolvable
     */
    private String resolveChargebeeCustomerId(Long localUserId) {
        User user = userRepository.findById(localUserId).orElse(null);
        if (user != null && user.getChargebeeCustomerId() != null
                && !user.getChargebeeCustomerId().isEmpty()) {
            logger.debug("Resolved Chargebee customer ID: {} for local user: {}",
                    user.getChargebeeCustomerId(), localUserId);
            return user.getChargebeeCustomerId();
        }

        Optional<CustomerEntity> customer = customerRepository.findById(localUserId.toString());
        if (customer.isPresent()) {
            logger.warn("Fallback to CustomerEntity lookup for local user: {}", localUserId);
            return customer.get().getId();
        }

        logger.error("No Chargebee customer ID found for local user: {}", localUserId);
        return null;
    }
}
