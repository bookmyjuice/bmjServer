package com.bookmyjuice.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.services.ChargebeeSyncService;

@RestController
@RequestMapping("/api/admin/chargebee-sync")
@PreAuthorize("hasRole('ADMIN')")  // Only admin users can access these endpoints
public class ChargebeeSyncController {

    private static final Logger logger = LoggerFactory.getLogger(ChargebeeSyncController.class);
    
    @Autowired
    private ChargebeeSyncService chargebeeSyncService;
    
    /**
     * Get the current sync status
     */
    @GetMapping("/status")
    public ResponseEntity<String> getSyncStatus() {
        try {
            String status = chargebeeSyncService.getSyncStatus();
            logger.info("Sync status requested: {}", status);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting sync status: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error getting sync status: " + e.getMessage());
        }
    }
    
    /**
     * Trigger a manual sync operation
     */
    @PostMapping("/sync")
    public ResponseEntity<String> triggerManualSync() {
        try {
            logger.info("Manual sync triggered by admin");
            
            // Run sync in a separate thread to avoid blocking the request
            new Thread(() -> {
                try {
                    chargebeeSyncService.performManualSync();
                } catch (Exception e) {
                    logger.error("Error during manual sync: {}", e.getMessage(), e);
                }
            }).start();
            
            return ResponseEntity.ok("Manual sync started. Check logs for progress.");
        } catch (Exception e) {
            logger.error("Error triggering manual sync: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error triggering manual sync: " + e.getMessage());
        }
    }
    
    /**
     * Health check endpoint for the sync service
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Chargebee Sync Service is running");
    }
}