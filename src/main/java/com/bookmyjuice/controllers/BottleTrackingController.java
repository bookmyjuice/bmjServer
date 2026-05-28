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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.models.User;
import com.bookmyjuice.models.dto.BottleLedgerEntry;
import com.bookmyjuice.models.entities.BottleTransactionEntity;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.services.BottleTrackingService;
import com.bookmyjuice.services.UserDetailsImpl;

/**
 * REST Controller for Bottle Tracking operations.
 * 
 * Provides endpoints for customers to view their bottle ledger,
 * transaction history, and report returns or broken bottles.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/bottles")
public class BottleTrackingController {

    private static final Logger logger = LoggerFactory.getLogger(BottleTrackingController.class);

    @Autowired
    private BottleTrackingService bottleTrackingService;

    @Autowired
    private UserRepository userRepository;

    // ==================== Query Endpoints ====================

    /**
     * GET /api/bottles/ledger
     * Returns the computed bottle ledger for the authenticated user.
     * Shows per-bottle-type balances (issued, returned, broken, outstanding).
     */
    @GetMapping("/ledger")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getLedger() {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String chargebeeCustomerId = user.getChargebeeCustomerId();
            if (chargebeeCustomerId == null || chargebeeCustomerId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Chargebee customer ID not found. Please ensure your profile is set up."));
            }

            List<BottleLedgerEntry> ledger = bottleTrackingService.getLedger(chargebeeCustomerId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", ledger));
        } catch (Exception e) {
            logger.error("Error fetching bottle ledger: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch bottle ledger: " + e.getMessage()));
        }
    }

    /**
     * GET /api/bottles/transactions
     * Returns the raw bottle transaction history for the authenticated user.
     */
    @GetMapping("/transactions")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getTransactions() {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String chargebeeCustomerId = user.getChargebeeCustomerId();
            if (chargebeeCustomerId == null || chargebeeCustomerId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Chargebee customer ID not found. Please ensure your profile is set up."));
            }

            List<BottleTransactionEntity> transactions = bottleTrackingService.getTransactions(chargebeeCustomerId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", transactions.size(),
                    "data", transactions));
        } catch (Exception e) {
            logger.error("Error fetching bottle transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch bottle transactions: " + e.getMessage()));
        }
    }

    // ==================== Mutation Endpoints ====================

    /**
     * POST /api/bottles/return
     * Record bottles returned by the customer.
     * 
     * Request body:
     * {
     *   "orderId": "cb_order_xxx",
     *   "bottleType": "glass_500ml",
     *   "quantity": 5
     * }
     */
    @PostMapping("/return")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> recordReturn(@RequestBody Map<String, Object> body) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String chargebeeCustomerId = user.getChargebeeCustomerId();
            if (chargebeeCustomerId == null || chargebeeCustomerId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Chargebee customer ID not found."));
            }

            String orderId = getStringParam(body, "orderId");
            String bottleType = getStringParam(body, "bottleType");
            int quantity = getIntParam(body, "quantity");

            if (orderId == null || bottleType == null || quantity <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "orderId, bottleType, and quantity (> 0) are required"));
            }

            BottleTransactionEntity tx = bottleTrackingService.recordReturn(
                    orderId, chargebeeCustomerId, bottleType, quantity, null,
                    "Customer-reported return");

            logger.info("Bottle return recorded: id={}, qty={}, type={}", tx.getId(), quantity, bottleType);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Bottle return recorded successfully",
                    "data", tx));
        } catch (Exception e) {
            logger.error("Error recording bottle return: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to record bottle return: " + e.getMessage()));
        }
    }

    /**
     * POST /api/bottles/broken
     * Report bottles as broken or lost.
     * 
     * Request body:
     * {
     *   "orderId": "cb_order_xxx",
     *   "bottleType": "glass_500ml",
     *   "quantity": 2
     * }
     */
    @PostMapping("/broken")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> recordBroken(@RequestBody Map<String, Object> body) {
        try {
            User user = getAuthenticatedUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String chargebeeCustomerId = user.getChargebeeCustomerId();
            if (chargebeeCustomerId == null || chargebeeCustomerId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Chargebee customer ID not found."));
            }

            String orderId = getStringParam(body, "orderId");
            String bottleType = getStringParam(body, "bottleType");
            int quantity = getIntParam(body, "quantity");

            if (orderId == null || bottleType == null || quantity <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "orderId, bottleType, and quantity (> 0) are required"));
            }

            BottleTransactionEntity tx = bottleTrackingService.recordBroken(
                    orderId, chargebeeCustomerId, bottleType, quantity, null,
                    "Customer-reported broken/lost");

            logger.info("Bottle broken recorded: id={}, qty={}, type={}", tx.getId(), quantity, bottleType);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Bottle broken/lost reported successfully",
                    "data", tx));
        } catch (Exception e) {
            logger.error("Error recording broken bottle: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to record broken bottle: " + e.getMessage()));
        }
    }

    // ==================== Helpers ====================

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userRepository.findById(userDetails.getId()).orElse(null);
        }
        return null;
    }

    private String getStringParam(Map<String, Object> body, String key) {
        Object val = body.get(key);
        return val instanceof String ? (String) val : null;
    }

    private int getIntParam(Map<String, Object> body, String key) {
        Object val = body.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }
}
