package com.bookmyjuice.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.models.entities.ItemEntity;
import com.bookmyjuice.repository.ItemRepository;
import com.bookmyjuice.services.ItemService;
import com.bookmyjuice.services.MetadataTestService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/test/metadata")
public class MetadataTestController {

    private static final Logger logger = LoggerFactory.getLogger(MetadataTestController.class);
    
    @Autowired
    private MetadataTestService metadataTestService;
    
    @Autowired
    private ItemService itemService;
    
    @Autowired
    private ItemRepository itemRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create a sample juice item with rich metadata
     * POST /api/test/metadata/create-sample
     */
    @PostMapping("/create-sample")
    public ResponseEntity<?> createSampleJuice() {
        try {
            ItemEntity item = metadataTestService.createJuiceWithMetadata();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sample juice created successfully");
            response.put("itemId", item.getId());
            response.put("metadata", item.getMetaData());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get item metadata in a structured format
     * GET /api/test/metadata/{itemId}
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<?> getItemMetadata(@PathVariable String itemId) {
        try {
            ItemEntity item = metadataTestService.displayItemMetadata(itemId);
            
            if (item == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Item not found");
                return ResponseEntity.status(404).body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("itemId", itemId);
            response.put("item", item);
            response.put("metadata", item.getMetaData());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Update item metadata
     * PUT /api/test/metadata/{itemId}
     */
    @PutMapping("/{itemId}")
    public ResponseEntity<?> updateItemMetadata(
            @PathVariable String itemId,
            @RequestParam(required = false) String imagePath,
            @RequestParam(required = false) Integer calories) {
        
        try {
            ItemEntity updatedItem = metadataTestService.updateItemMetadata(itemId, imagePath, calories);
            
            if (updatedItem == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Item not found");
                return ResponseEntity.status(404).body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Metadata updated successfully");
            response.put("itemId", updatedItem.getId());
            response.put("updatedMetadata", updatedItem.getMetaData());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Save custom metadata JSON
     * POST /api/test/metadata/{itemId}/custom
     */
    @PostMapping("/{itemId}/custom")
    public ResponseEntity<?> saveCustomMetadata(
            @PathVariable String itemId,
            @RequestBody Map<String, Object> customMetadata) {
        
        try {
            // Find the item (this should be moved to the service)
            ItemEntity item = new ItemEntity(); // You'd get this from repository
            item.setId(itemId);
            
            // Set the custom metadata
            item.setMetaDataFromObject(customMetadata);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Custom metadata saved");
            response.put("savedMetadata", item.getMetaData());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Create a test item with comprehensive Chargebee-style metadata
     * POST /api/test/metadata/create-test-item
     */
    @PostMapping("/create-test-item")
    public ResponseEntity<?> createTestItem(@RequestBody Map<String, String> request) {
        try {
            String itemId = request.get("itemId");
            String name = request.get("name");
            String description = request.get("description");
            
            if (itemId == null || name == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "itemId and name are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            ItemEntity item = itemService.createTestItemWithChargebeeMetadata(itemId, name, description);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test item created successfully");
            response.put("item", item);
            response.put("metadata", item.getMetaData());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Validate metadata for an item
     * GET /api/test/metadata/validate/{itemId}
     */
    @GetMapping("/validate/{itemId}")
    public ResponseEntity<?> validateMetadata(@PathVariable String itemId) {
        try {
            boolean isValid = itemService.validateItemMetadata(itemId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("itemId", itemId);
            response.put("isValid", isValid);
            response.put("message", isValid ? "Metadata is valid" : "Metadata is invalid or missing");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get detailed metadata information for debugging
     * GET /api/test/metadata/debug/{itemId}
     */
    @GetMapping("/debug/{itemId}")
    public ResponseEntity<?> debugMetadata(@PathVariable String itemId) {
        try {
            Map<String, Object> debugInfo = itemService.getItemMetadataInfo(itemId);
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Test processing Chargebee webhook payload directly
     * POST /api/test/metadata/test-chargebee-webhook
     */
    @PostMapping("/test-chargebee-webhook")
    public ResponseEntity<?> testChargebeeWebhook(@RequestBody Map<String, Object> webhookPayload) {
        try {
            logger.info("Testing Chargebee webhook payload: {}", webhookPayload);
            
            // Extract the item data from webhook payload
            @SuppressWarnings("unchecked")
            Map<String, Object> content = (Map<String, Object>) webhookPayload.get("content");
            @SuppressWarnings("unchecked")  
            Map<String, Object> itemData = (Map<String, Object>) content.get("item");
            
            if (itemData == null) {
                return ResponseEntity.badRequest().body("No item data found in webhook");
            }
            
            String itemId = (String) itemData.get("id");
            String name = (String) itemData.get("name");
            String description = (String) itemData.get("description");
            String status = (String) itemData.get("status");
            String type = (String) itemData.get("type");
            String unit = (String) itemData.get("unit");
            String itemFamilyId = (String) itemData.get("item_family_id");
            Boolean enabledForCheckout = (Boolean) itemData.get("enabled_for_checkout");
            Boolean enabledInPortal = (Boolean) itemData.get("enabled_in_portal");
            Object metadata = itemData.get("metadata");
            
            // Create or update the item in database
            ItemEntity entity = itemRepository.findById(itemId).orElse(new ItemEntity());
            entity.setId(itemId);
            entity.setName(name);
            entity.setDescription(description);
            entity.setStatus(status);
            entity.setType(type);
            entity.setUnit(unit);
            entity.setProductFamilyId(itemFamilyId);
            entity.setEnabledForCheckout(enabledForCheckout != null ? enabledForCheckout : false);
            entity.setEnabledInPortal(enabledInPortal != null ? enabledInPortal : false);
            
            // Convert metadata to JSON string
            String metadataJson = null;
            if (metadata != null) {
                try {
                    entity.setMetaDataFromObject(metadata);
                    logger.info("Converted metadata to JSON: {}", entity.getMetaData());
                } catch (Exception e) {
                    logger.error("Failed to convert metadata: {}", e.getMessage());
                    entity.setMetaData("{}");
                }
            }
            
            // Save to database
            ItemEntity savedEntity = itemRepository.save(entity);
            
            // Test the /charge-items endpoint to see if metadata is properly returned
            List<Map<String, Object>> juices = itemService.getJuices();
            Map<String, Object> testItem = juices.stream()
                .filter(juice -> itemId.equals(juice.get("juiceID")))
                .findFirst()
                .orElse(null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Webhook test completed");
            response.put("itemSaved", savedEntity != null);
            response.put("itemId", itemId);
            response.put("originalMetadata", metadata);
            response.put("savedMetadataJson", savedEntity.getMetaData());
            response.put("itemInChargeItems", testItem != null);
            response.put("extractedFields", testItem);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error testing webhook: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
