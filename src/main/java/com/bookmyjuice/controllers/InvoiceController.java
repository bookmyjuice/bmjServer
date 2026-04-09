package com.bookmyjuice.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.models.entities.InvoiceEntity;
import com.bookmyjuice.services.InvoiceApiService;
import com.bookmyjuice.services.UserDetailsImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for managing invoices
 * Handles invoice retrieval, PDF generation, and email sending
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

    @Autowired
    private InvoiceApiService invoiceApiService;

    /**
     * Get all invoices for the current user
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getMyInvoices() {
        try {
            String customerId = getCustomerIdFromSecurityContext();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            List<Map<String, Object>> invoices = invoiceApiService.getCustomerInvoices(customerId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", invoices
            ));
        } catch (Exception e) {
            logger.error("Error fetching invoices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get specific invoice details
     */
    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getInvoice(@PathVariable String invoiceId) {
        try {
            Map<String, Object> invoice = invoiceApiService.getInvoiceDetails(invoiceId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", invoice
            ));
        } catch (Exception e) {
            logger.error("Error fetching invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get invoice PDF URL
     */
    @GetMapping("/{invoiceId}/pdf-url")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getInvoicePdfUrl(@PathVariable String invoiceId) {
        try {
            String pdfUrl = invoiceApiService.getInvoicePdfUrl(invoiceId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "PDF URL retrieved",
                    "pdfUrl", pdfUrl
            ));
        } catch (Exception e) {
            logger.error("Error getting PDF URL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Send invoice email to customer
     */
    @PostMapping("/{invoiceId}/send-email")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> sendInvoiceEmail(@PathVariable String invoiceId,
            @RequestBody(required = false) Map<String, String> request) {
        try {
            String email = request != null ? request.get("email") : null;

            boolean success = invoiceApiService.sendInvoiceEmail(invoiceId, email);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Invoice email sent successfully"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Failed to send invoice email"));
            }
        } catch (Exception e) {
            logger.error("Error sending invoice email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get local invoices from database
     */
    @GetMapping("/local/history")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getLocalInvoiceHistory() {
        try {
            String customerId = getCustomerIdFromSecurityContext();
            if (customerId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            List<InvoiceEntity> invoices = invoiceApiService.getLocalCustomerInvoices(customerId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", invoices
            ));
        } catch (Exception e) {
            logger.error("Error fetching local invoices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get specific local invoice
     */
    @GetMapping("/local/{invoiceId}")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getLocalInvoice(@PathVariable String invoiceId) {
        try {
            Optional<InvoiceEntity> invoice = invoiceApiService.getLocalInvoice(invoiceId);

            if (invoice.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "data", invoice.get()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Invoice not found"));
            }
        } catch (Exception e) {
            logger.error("Error fetching local invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all invoices (admin endpoint)
     */
    @GetMapping("/admin/all-invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllInvoices() {
        try {
            List<InvoiceEntity> invoices = invoiceApiService.getAllInvoices();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "count", invoices.size(),
                    "data", invoices
            ));
        } catch (Exception e) {
            logger.error("Error fetching all invoices: {}", e.getMessage(), e);
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
