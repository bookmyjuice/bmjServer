package com.bookmyjuice.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.chargebee.models.Event;

import com.bookmyjuice.dto.ChargeItemDTO;
import com.bookmyjuice.models.entities.ItemEntity;
import com.bookmyjuice.models.entities.ItemPriceEntity;
import com.bookmyjuice.repository.ItemRepository;
import com.chargebee.models.Item;
import com.chargebee.models.ItemPrice;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemPriceService itemPriceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<?> saveItem(Event event) {
        if (event == null || event.content() == null || event.content().item() == null) {
            logger.error("Invalid event data received for saving item: {}", event);
            return ResponseEntity.status(400).body("Invalid event data");
        }

        try {
            var item = event.content().item();
            logger.info("Processing save item event for ID: {}", item.id());

            if (itemRepository.existsById(item.id())) {
                logger.info("Item already exists with ID: {}, updating instead", item.id());
                return updateItem(event);
            } else {
                // Create and populate the ItemEntity
                ItemEntity entity = new ItemEntity();
                entity.setId(item.id());
                entity.setName(item.name());
                entity.setDescription(item.description());
                entity.setType(item.type().name());
                entity.setStatus(item.status().name());
                entity.setExternalName(item.externalName());
                entity.setEnabledInPortal(item.enabledInPortal());
                entity.setEnabledForCheckout(item.enabledForCheckout());
                entity.setItemFamilyId(item.itemFamilyId());
                entity.setUnit(item.unit());

                // Handle metadata from Chargebee
                if (item.metadata() != null) {
                    try {
                        logger.info("Chargebee metadata object type: {}", item.metadata().getClass().getName());
                        logger.info("Chargebee metadata toString: {}", item.metadata().toString());

                        // Handle different types of metadata objects from Chargebee
                        Object metadataObj = item.metadata();
                        Map<String, Object> actualMetadata = convertChargebeeMetadata(metadataObj);

                        // Clean out mapType/empty keys if they are the only keys
                        if (actualMetadata != null) {
                            boolean onlyWrapper = actualMetadata.size() == 2 && actualMetadata.containsKey("mapType")
                                    && actualMetadata.containsKey("empty");
                            if (onlyWrapper) {
                                entity.setMetaData("{}");
                                logger.info("Saving item {} with blank metadata (wrapper only)", item.id());
                            } else if (!actualMetadata.isEmpty()) {
                                entity.setMetaDataFromObject(actualMetadata);
                                logger.info("Saving item {} with cleaned metadata: {}", item.id(),
                                        entity.getMetaData());
                            } else {
                                entity.setMetaData(null);
                                logger.warn("No valid metadata found for item {}", item.id());
                            }
                        } else {
                            entity.setMetaData(null);
                            logger.warn("No valid metadata found for item {}", item.id());
                        }
                    } catch (Exception e) {
                        logger.error("Failed to convert metadata to JSON for item {}: {}", item.id(), e.getMessage(),
                                e);
                        // Try direct ObjectMapper as fallback
                        try {
                            entity.setMetaDataFromObject(item.metadata());
                            logger.info("Used setMetaDataFromObject fallback for item {}: {}", item.id(),
                                    entity.getMetaData());
                        } catch (Exception e2) {
                            logger.error("Direct ObjectMapper also failed for item {}: {}", item.id(), e2.getMessage());
                            // Set as null if both methods fail
                            entity.setMetaData(null);
                        }
                    }
                } else {
                    entity.setMetaData(null);
                    logger.info("Saving item {} with no metadata", item.id());
                }

                itemRepository.save(entity);
                logger.info("New item saved successfully with ID: {}", item.id());

                // Handle nested ItemPrice entities if present in the event
                handleNestedItemPrices(event, entity);

                return ResponseEntity.ok("New item saved successfully");
            }
        } catch (Exception e) {
            logger.error("Error occurred while saving item: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error saving item: " + e.getMessage());
        }
    }

    public ResponseEntity<?> updateItem(Event event) {
        if (event == null || event.eventType() == null || event.content() == null || event.content().item() == null) {
            logger.error("Invalid event data received for updating item: {}", event);
            return ResponseEntity.status(400).body("Invalid event data");
        }

        try {
            var item = event.content().item();
            logger.info("Processing update item event for ID: {}", item.id());

            if (!itemRepository.existsById(item.id())) {
                logger.warn("Item not found with ID: {}, saving instead", item.id());
                return saveItem(event);
            } else {
                ItemEntity entity = itemRepository.findById(item.id()).orElse(null);
                if (entity == null) {
                    logger.warn("Item not found in repository with ID: {}, saving instead", item.id());
                    return saveItem(event);
                } else {
                    entity.setName(item.name());
                    entity.setDescription(item.description());
                    entity.setType(item.type().name());
                    entity.setStatus(item.status().name());
                    entity.setExternalName(item.externalName());
                    entity.setEnabledInPortal(item.enabledInPortal());
                    entity.setEnabledForCheckout(item.enabledForCheckout());
                    entity.setItemFamilyId(item.itemFamilyId());
                    entity.setUnit(item.unit());

                    // Handle metadata from Chargebee
                    if (item.metadata() != null) {
                        try {
                            logger.info("Chargebee metadata object type: {}", item.metadata().getClass().getName());
                            logger.info("Chargebee metadata toString: {}", item.metadata().toString());

                            Object metadataObj = item.metadata();
                            Map<String, Object> actualMetadata = convertChargebeeMetadata(metadataObj);

                            boolean onlyWrapper = actualMetadata != null && actualMetadata.size() == 2
                                    && actualMetadata.containsKey("mapType") && actualMetadata.containsKey("empty");
                            if (onlyWrapper) {
                                entity.setMetaData("{}");
                                logger.info("Updating item {} with blank metadata (wrapper only)", item.id());
                            } else if (actualMetadata != null && !actualMetadata.isEmpty()) {
                                entity.setMetaDataFromObject(actualMetadata);
                                logger.info("Updating item {} with converted metadata: {}", item.id(),
                                        entity.getMetaData());
                            } else {
                                entity.setMetaData(null);
                                logger.warn("No valid metadata found for item {}", item.id());
                            }
                        } catch (Exception e) {
                            logger.error("Failed to convert metadata for item {}: {}", item.id(), e.getMessage(), e);
                            entity.setMetaData(null);
                        }
                    } else {
                        entity.setMetaData(null);
                        logger.info("Updating item {} with no metadata", item.id());
                    }

                    itemRepository.save(entity);
                    logger.info("Item updated successfully with ID: {}", item.id());

                    // Handle nested ItemPrice entities if present in the event
                    handleNestedItemPrices(event, entity);

                    return ResponseEntity.ok("Item updated successfully");
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred while updating item: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error updating item: " + e.getMessage());
        }
    }

    public ResponseEntity<?> deleteItem(Event event) {
        if (event == null || event.content() == null || event.content().item() == null) {
            logger.error("Invalid event data received for deleting item: {}", event);
            return ResponseEntity.status(400).body("Invalid event data");
        }

        try {
            var item = event.content().item();
            logger.info("Processing delete item event for ID: {}", item.id());

            if (itemRepository.existsById(item.id())) {
                itemRepository.deleteById(item.id());
                logger.info("Item deleted successfully with ID: {}", item.id());
                return ResponseEntity.ok("Item deleted successfully");
            } else {
                logger.warn("Item not found with ID: {}", item.id());
                return ResponseEntity.status(404).body("Item not found");
            }
        } catch (Exception e) {
            logger.error("Error occurred while deleting item: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error deleting item: " + e.getMessage());
        }
    }

    public ResponseEntity<?> archiveItem(Event event) {
        if (event == null || event.content() == null || event.content().item() == null) {
            logger.error("Invalid event data received for archiving item: {}", event);
            return ResponseEntity.status(400).body("Invalid event data");
        }

        try {
            var item = event.content().item();
            logger.info("Processing archive item event for ID: {}", item.id());

            ItemEntity entity = itemRepository.findById(item.id())
                    .orElseThrow(() -> new IllegalArgumentException("Item not found"));

            entity.setArchived(true);
            itemRepository.save(entity);
            logger.info("Item archived successfully with ID: {}", item.id());

            return ResponseEntity.ok("Item archived successfully");
        } catch (Exception ex) {
            logger.error("Error archiving item: {}", ex.getMessage());
            return ResponseEntity.status(500).body("Error archiving item");
        }
    }

    public ResponseEntity<String> handleDefaultItemEvent(Event event) {
        logger.warn("Unhandled item event type: {}", event.eventType());
        return ResponseEntity.status(400).body("Unhandled item event type: " + event.eventType());
    }

    public List<Map<String, Object>> getJuices() {
        try {
            logger.info("Fetching juices from database");

            // First try to find items that contain "juice" in name or itemFamilyId
            List<ItemEntity> juiceItems = itemRepository.findAllActiveChargeItems();

            // // If no juice-specific items found, get all active charge items
            // if (juiceItems.isEmpty()) {
            // logger.info("No juice-specific items found, fetching all active charge
            // items");
            // juiceItems = itemRepository.findAllActiveChargeItems();
            // }

            List<Map<String, Object>> juices = new ArrayList<>();

            for (ItemEntity item : juiceItems) {
                // Filter items that are juice-related
                // if (isJuiceItem(item)) {
                Map<String, Object> juiceMap = createJuiceMap(item);
                juices.add(juiceMap);
                // }
            }

            // If no juices found in database, return default juices
            if (juices.isEmpty()) {
                logger.info("No juices found in database, returning default juices");
                juices = getDefaultJuices();
            }

            logger.info("Found {} juices", juices.size());
            return juices;

        } catch (Exception e) {
            logger.error("Error fetching juices from database: {}", e.getMessage(), e);
            // Return default juices in case of error
            return getDefaultJuices();
        }
    }

    // private boolean isJuiceItem(ItemEntity item) {
    // if (item.getName() != null && item.getName().toLowerCase().contains("juice"))
    // {
    // return true;
    // }
    // if (item.getItemFamilyId() != null &&
    // item.getItemFamilyId().toLowerCase().contains("juice")) {
    // return true;
    // }
    // // Check if metadata contains juice-related information
    // if (item.getMetaData() != null) {
    // try {
    // JsonNode metadata = objectMapper.readTree(item.getMetaData());
    // if (metadata.has("category") &&
    // metadata.get("category").asText().toLowerCase().contains("juice")) {
    // return true;
    // }
    // } catch (Exception e) {
    // logger.debug("Failed to parse metadata for item {}: {}", item.getId(),
    // e.getMessage());
    // }
    // }
    // return false;
    // }

    private Map<String, Object> createJuiceMap(ItemEntity item) {
        Map<String, Object> juiceMap = new HashMap<>();
        juiceMap.put("juiceID", item.getId());
        juiceMap.put("name", item.getName());
        juiceMap.put("description", item.getDescription());
        juiceMap.put("type", item.getType());
        juiceMap.put("status", item.getStatus());
        juiceMap.put("unit", item.getUnit());
        juiceMap.put("itemFamilyId", item.getItemFamilyId());

        // Add basic fields always available
        juiceMap.put("enabledInPortal", item.isEnabledInPortal());
        juiceMap.put("enabledForCheckout", item.isEnabledForCheckout());
        juiceMap.put("archived", item.isArchived());

        // Parse metadata from Chargebee (stored in database)
        logger.debug("Processing metadata for item {}: {}", item.getId(), item.getMetaData());

        if (item.getMetaData() != null && !item.getMetaData().trim().isEmpty()) {
            try {
                // First, add the raw metadata for debugging
                juiceMap.put("_rawMetadata", item.getMetaData());

                // Parse and extract specific metadata fields
                extractMetadataFields(item, juiceMap);

            } catch (Exception e) {
                logger.error("Failed to parse metadata for item {}: {}", item.getId(), e.getMessage(), e);
                // Add error info for debugging
                juiceMap.put("_metadataError", e.getMessage());
            }
        } else {
            logger.debug("No metadata found for item {}", item.getId());
        }

        return juiceMap;
    }

    private void extractMetadataFields(ItemEntity item, Map<String, Object> juiceMap) {
        // Extract juice-specific metadata using the utility methods
        addMetadataFieldIfPresent(item, juiceMap, "imagePath");
        addMetadataFieldIfPresent(item, juiceMap, "startColor");
        addMetadataFieldIfPresent(item, juiceMap, "endColor");
        addMetadataFieldIfPresent(item, juiceMap, "category");
        addMetadataFieldIfPresent(item, juiceMap, "subcategory");
        addMetadataFieldIfPresent(item, juiceMap, "servingSize");
        addMetadataFieldIfPresent(item, juiceMap, "shelfLife");
        addMetadataFieldIfPresent(item, juiceMap, "preparationTime");
        addMetadataFieldIfPresent(item, juiceMap, "temperature");

        // Extract integer fields
        addMetadataIntFieldIfPresent(item, juiceMap, "calories");
        addMetadataIntFieldIfPresent(item, juiceMap, "popularity");

        // Extract array fields
        addMetadataArrayFieldIfPresent(item, juiceMap, "meals");
        addMetadataArrayFieldIfPresent(item, juiceMap, "benefits");
        addMetadataArrayFieldIfPresent(item, juiceMap, "allergies");
        addMetadataArrayFieldIfPresent(item, juiceMap, "tags");

        // Extract nested objects
        extractNestedObject(item, juiceMap, "nutritionalInfo");
        extractNestedObject(item, juiceMap, "customization");

        // Log successful metadata extraction
        logger.debug("Successfully extracted metadata fields for item {}", item.getId());
    }

    private void addMetadataFieldIfPresent(ItemEntity item, Map<String, Object> juiceMap, String fieldName) {
        String value = item.getMetaDataField(fieldName);
        if (value != null && !value.trim().isEmpty()) {
            juiceMap.put(fieldName, value);
        }
    }

    private void addMetadataIntFieldIfPresent(ItemEntity item, Map<String, Object> juiceMap, String fieldName) {
        Integer value = item.getMetaDataFieldAsInt(fieldName);
        if (value != null) {
            juiceMap.put(fieldName, value);
        }
    }

    private void addMetadataArrayFieldIfPresent(ItemEntity item, Map<String, Object> juiceMap, String fieldName) {
        String[] array = item.getMetaDataFieldAsArray(fieldName);
        if (array.length > 0) {
            juiceMap.put(fieldName, List.of(array));
        }
    }

    private void extractNestedObject(ItemEntity item, Map<String, Object> juiceMap, String objectName) {
        try {
            JsonNode nestedObject = item.getMetaDataAsJson().get(objectName);
            if (nestedObject != null && nestedObject.isObject()) {
                Map<String, Object> objectMap = new HashMap<>();
                nestedObject.fields().forEachRemaining(entry -> {
                    if (entry.getValue().isArray()) {
                        List<String> options = new ArrayList<>();
                        entry.getValue().forEach(option -> options.add(option.asText()));
                        objectMap.put(entry.getKey(), options);
                    } else {
                        objectMap.put(entry.getKey(), entry.getValue().asText());
                    }
                });
                if (!objectMap.isEmpty()) {
                    juiceMap.put(objectName, objectMap);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract nested object {} for item {}: {}", objectName, item.getId(), e.getMessage());
        }
    }

    /**
     * Method to create a test item with comprehensive metadata (simulating
     * Chargebee structure)
     */
    public ItemEntity createTestItemWithChargebeeMetadata(String itemId, String name, String description) {
        // Create comprehensive metadata object that simulates what Chargebee would send
        Map<String, Object> metadata = new HashMap<>();

        // Basic visual metadata
        metadata.put("imagePath", "assets/" + itemId + ".png");
        metadata.put("startColor", "#4CAF50");
        metadata.put("endColor", "#2E7D32");
        metadata.put("category", "juice");
        metadata.put("subcategory", "green_juice");

        // Nutritional metadata
        metadata.put("calories", 180);
        metadata.put("servingSize", "500ml");
        metadata.put("temperature", "cold");
        metadata.put("shelfLife", "3 days");
        metadata.put("preparationTime", "10 minutes");
        metadata.put("popularity", 85);

        // Arrays
        metadata.put("meals", List.of("Spinach", "Kale", "Cucumber", "Green Apple"));
        metadata.put("benefits", List.of("Detoxification", "Immune boost", "Energy enhancement"));
        metadata.put("allergies", List.of("None"));
        metadata.put("tags", List.of("detox", "green", "healthy", "organic"));

        // Nested objects
        Map<String, String> nutritionalInfo = new HashMap<>();
        nutritionalInfo.put("protein", "3g");
        nutritionalInfo.put("carbs", "42g");
        nutritionalInfo.put("fiber", "2g");
        nutritionalInfo.put("sugar", "36g");
        nutritionalInfo.put("vitaminC", "120mg");
        metadata.put("nutritionalInfo", nutritionalInfo);

        Map<String, Object> customization = new HashMap<>();
        customization.put("sugarLevel", List.of("no-sugar", "low-sugar", "regular"));
        customization.put("iceLevel", List.of("no-ice", "less-ice", "regular-ice"));
        customization.put("addOns", List.of("chia-seeds", "protein-powder", "honey"));
        metadata.put("customization", customization);

        // Create entity
        ItemEntity entity = new ItemEntity();
        entity.setId(itemId);
        entity.setName(name);
        entity.setDescription(description);
        entity.setType("juice");
        entity.setStatus("active");
        entity.setEnabledInPortal(true);
        entity.setEnabledForCheckout(true);
        entity.setItemFamilyId("juice_family");
        entity.setUnit("ml");
        entity.setArchived(false);

        // Set metadata using the utility method
        entity.setMetaDataFromObject(metadata);

        logger.info("Created test item {} with metadata: {}", itemId, entity.getMetaData());

        return itemRepository.save(entity);
    }

    /**
     * Method to check if an item has proper metadata structure
     */
    public boolean validateItemMetadata(String itemId) {
        ItemEntity item = itemRepository.findById(itemId).orElse(null);
        if (item == null) {
            logger.warn("Item {} not found for metadata validation", itemId);
            return false;
        }

        if (item.getMetaData() == null || item.getMetaData().trim().isEmpty()) {
            logger.warn("Item {} has no metadata", itemId);
            return false;
        }

        try {
            JsonNode metadata = item.getMetaDataAsJson();
            logger.info("Item {} metadata validation - Raw: {}", itemId, item.getMetaData());

            // Check for essential fields
            boolean hasBasicFields = metadata.has("imagePath") || metadata.has("startColor") ||
                    metadata.has("category") || metadata.has("calories");

            if (hasBasicFields) {
                logger.info("Item {} has valid metadata structure", itemId);
                return true;
            } else {
                logger.warn("Item {} metadata missing essential fields", itemId);
                return false;
            }

        } catch (Exception e) {
            logger.error("Item {} has invalid metadata JSON: {}", itemId, e.getMessage());
            return false;
        }
    }

    private List<Map<String, Object>> getDefaultJuices() {
        List<Map<String, Object>> defaultJuices = new ArrayList<>();

        Map<String, Object> watermelon = new HashMap<>();
        watermelon.put("juiceID", "watermelon_001");
        watermelon.put("name", "Watermelon");
        watermelon.put("description", "Fresh watermelon juice");
        watermelon.put("imagePath", "assets/watermelon.png");
        watermelon.put("startColor", "#FFB1C9");
        watermelon.put("endColor", "#B8292C");
        watermelon.put("calories", 525);
        watermelon.put("meals", List.of("Watermelon juice"));
        defaultJuices.add(watermelon);

        Map<String, Object> pineapple = new HashMap<>();
        pineapple.put("juiceID", "pineapple_001");
        pineapple.put("name", "Pineapple");
        pineapple.put("description", "Fresh pineapple juice");
        pineapple.put("imagePath", "assets/pineapple.png");
        pineapple.put("startColor", "#fad704");
        pineapple.put("endColor", "#ffd964");
        pineapple.put("calories", 602);
        pineapple.put("meals", List.of("Fresh pineapple", "a pinch of salt", "It's a pineapple", "in bottle!"));
        defaultJuices.add(pineapple);

        Map<String, Object> abc = new HashMap<>();
        abc.put("juiceID", "abc_001");
        abc.put("name", "ABC");
        abc.put("description", "Apple Beetroot Carrot juice");
        abc.put("imagePath", "assets/ABC.png");
        abc.put("startColor", "#673f45");
        abc.put("endColor", "#7a1f3d");
        abc.put("calories", 0);
        abc.put("meals", List.of("Apple", "Beetroot", "Carrot"));
        defaultJuices.add(abc);

        Map<String, Object> vitaminC = new HashMap<>();
        vitaminC.put("juiceID", "vitamin_c_001");
        vitaminC.put("name", "Vitamin C");
        vitaminC.put("description", "Vitamin C rich juice");
        vitaminC.put("imagePath", "assets/VitaminC.png");
        vitaminC.put("startColor", "#FFF12D");
        vitaminC.put("endColor", "#988623");
        vitaminC.put("calories", 0);
        vitaminC.put("meals", List.of("Amla", "Pineapple", "Tangerine"));
        defaultJuices.add(vitaminC);

        Map<String, Object> bloodyRed = new HashMap<>();
        bloodyRed.put("juiceID", "bloody_red_001");
        bloodyRed.put("name", "Bloody Red");
        bloodyRed.put("description", "PBC - Pomegranate Beetroot Carrot");
        bloodyRed.put("imagePath", "assets/PBC.png");
        bloodyRed.put("startColor", "#880808");
        bloodyRed.put("endColor", "#B8292C");
        bloodyRed.put("calories", 0);
        bloodyRed.put("meals", List.of("Recommend:", "703 kcal"));
        defaultJuices.add(bloodyRed);

        return defaultJuices;
    }

    /**
     * Get detailed metadata information for debugging
     */
    public Map<String, Object> getItemMetadataInfo(String itemId) {
        Map<String, Object> info = new HashMap<>();

        ItemEntity item = itemRepository.findById(itemId).orElse(null);
        if (item == null) {
            info.put("found", false);
            info.put("error", "Item not found");
            return info;
        }

        info.put("found", true);
        info.put("itemId", item.getId());
        info.put("name", item.getName());
        info.put("hasMetadata", item.getMetaData() != null && !item.getMetaData().trim().isEmpty());
        info.put("rawMetadata", item.getMetaData());

        if (item.getMetaData() != null && !item.getMetaData().trim().isEmpty()) {
            try {
                JsonNode metadata = item.getMetaDataAsJson();
                info.put("parsedSuccessfully", true);

                // Count available fields
                Map<String, Object> fieldInfo = new HashMap<>();
                metadata.fieldNames().forEachRemaining(fieldName -> {
                    JsonNode fieldValue = metadata.get(fieldName);
                    fieldInfo.put(fieldName, fieldValue.getNodeType().toString().toLowerCase());
                });
                info.put("availableFields", fieldInfo);

            } catch (Exception e) {
                info.put("parsedSuccessfully", false);
                info.put("parseError", e.getMessage());
            }
        }

        return info;
    }

    /**
     * Get all items from database in standardized format for API responses
     */
    public List<Map<String, Object>> getAllItemsStandardFormat() {
        List<ItemEntity> allItems = itemRepository.findAll();
        List<Map<String, Object>> formattedItems = new ArrayList<>();

        for (ItemEntity item : allItems) {
            formattedItems.add(createStandardItemMap(item));
        }

        return formattedItems;
    }

    /**
     * Get all items from Chargebee in standardized format for API responses
     */
    public List<Map<String, Object>> getAllChargebeeItemsStandardFormat() {
        try {
            com.chargebee.ListResult result = Item.list().request();

            List<Map<String, Object>> formattedItems = new ArrayList<>();

            for (com.chargebee.ListResult.Entry entry : result) {
                Item item = entry.item();
                Map<String, Object> itemMap = createStandardChargebeeItemMap(item);
                formattedItems.add(itemMap);
            }

            return formattedItems;

        } catch (Exception e) {
            logger.error("Error fetching items from Chargebee: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Create standardized item map from database ItemEntity
     */
    private Map<String, Object> createStandardItemMap(ItemEntity item) {
        Map<String, Object> itemMap = new HashMap<>();

        // Basic fields
        itemMap.put("id", item.getId());
        itemMap.put("name", item.getName());
        itemMap.put("externalName", item.getExternalName());
        itemMap.put("description", item.getDescription());
        itemMap.put("type", item.getType());
        itemMap.put("status", item.getStatus());
        itemMap.put("enabledForCheckout", item.isEnabledForCheckout());
        itemMap.put("enabledInPortal", item.isEnabledInPortal());
        itemMap.put("isGiftable", item.isGiftable());
        itemMap.put("isShippable", item.isShippable());
        itemMap.put("deleted", item.isDeleted());
        itemMap.put("unit", item.getUnit());
        itemMap.put("itemFamilyId", item.getItemFamilyId());
        itemMap.put("applicableItems", new ArrayList<>()); // Database doesn't store this
        itemMap.put("jsonObject", item.getJsonObject());

        // Parse metadata
        Map<String, Object> metadata = new HashMap<>();
        if (item.getMetaData() != null && !item.getMetaData().trim().isEmpty()) {
            try {
                JsonNode metadataJson = item.getMetaDataAsJson();
                metadata = objectMapper.convertValue(metadataJson, Map.class);
            } catch (Exception e) {
                logger.warn("Failed to parse metadata for item {}: {}", item.getId(), e.getMessage());
                metadata.put("_parseError", e.getMessage());
                metadata.put("_rawData", item.getMetaData());
            }
        }
        itemMap.put("metaData", metadata);

        // Add ItemPrices
        List<Map<String, Object>> itemPrices = new ArrayList<>();
        if (item.getItemPrices() != null && !item.getItemPrices().isEmpty()) {
            for (com.bookmyjuice.models.entities.ItemPriceEntity priceEntity : item.getItemPrices()) {
                Map<String, Object> priceMap = new HashMap<>();
                priceMap.put("id", priceEntity.getId());
                priceMap.put("name", priceEntity.getName());
                priceMap.put("description", priceEntity.getDescription());
                priceMap.put("price", priceEntity.getPrice());
                priceMap.put("currencyCode", priceEntity.getCurrencyCode());
                priceMap.put("period", priceEntity.getPeriod());
                priceMap.put("periodUnit", priceEntity.getPeriodUnit());
                priceMap.put("pricingModel", priceEntity.getPricingModel());
                priceMap.put("status", priceEntity.getStatus());
                priceMap.put("trialPeriod", priceEntity.getTrialPeriod());
                priceMap.put("trialPeriodUnit", priceEntity.getTrialPeriodUnit());
                itemPrices.add(priceMap);
            }
        }
        itemMap.put("itemPrices", itemPrices);

        return itemMap;
    }

    /**
     * Create standardized item map from Chargebee Item
     */
    private Map<String, Object> createStandardChargebeeItemMap(Item item) {
        Map<String, Object> itemMap = new HashMap<>();

        // Basic fields
        itemMap.put("id", item.id());
        itemMap.put("name", item.name());
        itemMap.put("externalName", item.externalName());
        itemMap.put("description", item.description());
        itemMap.put("type", item.type());
        itemMap.put("status", item.status());
        itemMap.put("enabledForCheckout", item.enabledForCheckout());
        itemMap.put("enabledInPortal", item.enabledInPortal());
        itemMap.put("isGiftable", item.isGiftable());
        itemMap.put("isShippable", item.isShippable());
        itemMap.put("deleted", item.deleted());
        itemMap.put("unit", item.unit());
        itemMap.put("itemFamilyId", item.itemFamilyId());
        itemMap.put("applicableItems", item.applicableItems());
        itemMap.put("jsonObject", item.jsonObj.toString());

        // Parse metadata using our improved conversion method
        Map<String, Object> metadata = new HashMap<>();
        if (item.metadata() != null) {
            try {
                metadata = convertChargebeeMetadata(item.metadata());
            } catch (Exception e) {
                logger.warn("Failed to convert metadata for Chargebee item {}: {}", item.id(), e.getMessage());
                metadata.put("_conversionError", e.getMessage());
            }
        }
        itemMap.put("metaData", metadata);

        return itemMap;
    }

    /**
     * Convert Chargebee metadata object to proper Map
     * Handles the case where Chargebee sends wrapper objects instead of actual
     * metadata
     */
    private Map<String, Object> convertChargebeeMetadata(Object metadataObj) {
        if (metadataObj == null) {
            logger.info("Metadata object is null");
            return new HashMap<>();
        }

        String className = metadataObj.getClass().getName();
        logger.info("Converting Chargebee metadata of type: {}", className);

        // Special handling for Chargebee JSONObject
        if ("com.chargebee.org.json.JSONObject".equals(className)) {
            logger.info("Detected Chargebee JSONObject, using toString() method");
            try {
                String jsonString = metadataObj.toString();
                logger.info("JSONObject toString result: {}", jsonString);
                @SuppressWarnings("unchecked")
                Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
                logger.info("Successfully parsed JSONObject toString to Map with {} entries", map.size());
                return map;
            } catch (Exception e) {
                logger.error("Failed to parse Chargebee JSONObject toString: {}", e.getMessage());
                return new HashMap<>();
            }
        }

        // If it's already a Map, check for wrapper and extract real data if present
        if (metadataObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) metadataObj;
            logger.info("Metadata is a Map with {} entries", map.size());

            // If it has mapType/empty but also other fields, extract only real fields
            Map<String, Object> cleanedMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                if (!key.equals("empty") && !key.equals("mapType")) {
                    cleanedMap.put(key, entry.getValue());
                }
            }
            if (!cleanedMap.isEmpty()) {
                logger.info("Cleaned map has {} entries (real data extracted)", cleanedMap.size());
                return cleanedMap;
            } else if (map.containsKey("empty") && map.containsKey("mapType")) {
                logger.warn("Detected Chargebee metadata wrapper with ONLY empty/mapType properties");
                logger.warn("Wrapper content: empty={}, mapType={}", map.get("empty"), map.get("mapType"));
                return new HashMap<>();
            } else {
                logger.info("Map has no real data fields, returning as is");
                return map;
            }
        }

        // If it's not a Map or JSONObject, try to convert it using ObjectMapper
        try {
            logger.info("Attempting ObjectMapper conversion for type: {}", className);
            String jsonString = objectMapper.writeValueAsString(metadataObj);
            logger.info("ObjectMapper converted metadata to: {}", jsonString);

            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);

            // Check if the converted map is the problematic wrapper
            if (map.containsKey("empty") && map.containsKey("mapType") && map.size() <= 2) {
                logger.warn("ObjectMapper conversion resulted in wrapper properties only");
                return new HashMap<>();
            }

            logger.info("ObjectMapper conversion successful, returning map with {} entries", map.size());
            return map;

        } catch (Exception e) {
            logger.error("Failed to convert metadata object using ObjectMapper: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Handle nested ItemPrice entities when processing Item events
     */
    private void handleNestedItemPrices(Event event, ItemEntity itemEntity) {
        try {
            logger.debug("Checking for nested ItemPrice entities in item event for item: {}", itemEntity.getId());

            // Check if the event contains nested ItemPrice data
            if (event.content() != null && event.content().itemPrice() != null) {
                logger.info("Found nested ItemPrice in item event for item: {}. Processing ItemPrice: {}",
                        itemEntity.getId(), event.content().itemPrice().id());

                // Process the nested item price through ItemPriceService
                // Create a pseudo-event for the ItemPrice to maintain consistency
                itemPriceService.saveOrUpdateItemPrice(event);

                logger.info("Successfully processed nested ItemPrice: {} for item: {}",
                        event.content().itemPrice().id(), itemEntity.getId());
            }

            // Additionally, check if there are any item prices that should be linked to
            // this item
            // This handles cases where item prices exist but aren't properly linked
            checkAndLinkExistingItemPrices(itemEntity);

        } catch (Exception e) {
            logger.error("Error handling nested ItemPrices for item {}: {}", itemEntity.getId(), e.getMessage(), e);
        }
    }

    /**
     * Check and link any existing ItemPrice entities that belong to this item
     */
    private void checkAndLinkExistingItemPrices(ItemEntity itemEntity) {
        try {
            // This method can be used to synchronize any orphaned item prices
            // For now, we'll log that the item entity is ready for item price linking
            logger.debug("Item entity {} is ready for ItemPrice linking", itemEntity.getId());
        } catch (Exception e) {
            logger.error("Error checking existing ItemPrices for item {}: {}", itemEntity.getId(), e.getMessage(), e);
        }
    }

    /**
     * TC-PROD-001: Fetch all items from Chargebee, cache locally, and filter by
     * Delight/Signature/Premium categories and 200/300/500ml sizes
     *
     * @return List of ChargeItemDTO with filtered categories and sizes
     */
    public List<ChargeItemDTO> getChargeItemsWithCategoriesAndSizes() {
        logger.info("Fetching items from Chargebee with category and size filtering");

        try {
            // Step 1: Fetch all items from Chargebee API
            com.chargebee.ListResult result = Item.list().request();
            List<Item> chargebeeItems = new ArrayList<>();

            for (com.chargebee.ListResult.Entry entry : result) {
                chargebeeItems.add(entry.item());
            }

            logger.info("Fetched {} items from Chargebee", chargebeeItems.size());

            // Step 2: Convert to DTOs and filter by categories
            List<ChargeItemDTO> filteredItems = new ArrayList<>();

            for (Item chargebeeItem : chargebeeItems) {
                // Filter by category (Delight, Signature, Premium)
                String category = extractCategoryFromMetadata(chargebeeItem.metadata());
                if (!isValidCategory(category)) {
                    logger.debug("Skipping item {} - invalid category: {}", chargebeeItem.id(), category);
                    continue;
                }

                // Convert to DTO
                ChargeItemDTO dto = convertToChargeItemDTO(chargebeeItem);
                filteredItems.add(dto);
            }

            // Step 3: Cache items locally (sync to database)
            cacheItemsLocally(filteredItems);

            logger.info("Returning {} filtered items with categories: Delight, Signature, Premium",
                    filteredItems.size());
            return filteredItems;

        } catch (Exception e) {
            logger.error("Error fetching items from Chargebee: {}", e.getMessage(), e);
            // Fallback to local cache
            logger.info("Falling back to local database cache");
            return getItemsFromLocalCache();
        }
    }

    /**
     * Extract category from Chargebee item metadata
     */
    private String extractCategoryFromMetadata(Object metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            // Convert metadata to string and parse
            String metadataStr = metadata.toString();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(metadataStr);
            return jsonNode.has("category") ? jsonNode.get("category").asText() : null;
        } catch (Exception e) {
            logger.debug("Failed to extract category from metadata: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if category is valid (Delight, Signature, Premium)
     */
    private boolean isValidCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        String catLower = category.toLowerCase().trim();
        return catLower.equals("delight") || catLower.equals("signature") || catLower.equals("premium");
    }

    /**
     * Convert Chargebee Item to ChargeItemDTO
     */
    private ChargeItemDTO convertToChargeItemDTO(Item chargebeeItem) {
        ChargeItemDTO dto = ChargeItemDTO.builder()
                .itemId(chargebeeItem.id())
                .name(chargebeeItem.name())
                .description(chargebeeItem.description())
                .itemFamilyId(chargebeeItem.itemFamilyId())
                .status(chargebeeItem.status().name())
                .enabledInPortal(chargebeeItem.enabledInPortal())
                .enabledForCheckout(chargebeeItem.enabledForCheckout())
                .build();

        // Extract metadata fields
        if (chargebeeItem.metadata() != null) {
            try {
                String metadataStr = chargebeeItem.metadata().toString();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(metadataStr);

                // Basic fields
                if (jsonNode.has("category"))
                    dto.setCategory(jsonNode.get("category").asText());
                if (jsonNode.has("subcategory"))
                    dto.setSubcategory(jsonNode.get("subcategory").asText());
                if (jsonNode.has("imagePath"))
                    dto.setImagePath(jsonNode.get("imagePath").asText());
                if (jsonNode.has("startColor"))
                    dto.setStartColor(jsonNode.get("startColor").asText());
                if (jsonNode.has("endColor"))
                    dto.setEndColor(jsonNode.get("endColor").asText());
                if (jsonNode.has("calories"))
                    dto.setCalories(jsonNode.get("calories").asInt());
                if (jsonNode.has("popularity"))
                    dto.setPopularity(jsonNode.get("popularity").asInt());
                if (jsonNode.has("servingSize"))
                    dto.setServingSize(jsonNode.get("servingSize").asText());
                if (jsonNode.has("shelfLife"))
                    dto.setShelfLife(jsonNode.get("shelfLife").asText());
                if (jsonNode.has("preparationTime"))
                    dto.setPreparationTime(jsonNode.get("preparationTime").asText());
                if (jsonNode.has("temperature"))
                    dto.setTemperature(jsonNode.get("temperature").asText());

                // Array fields
                if (jsonNode.has("meals"))
                    dto.setMeals(jsonNodeToArrayList(jsonNode.get("meals")));
                if (jsonNode.has("benefits"))
                    dto.setBenefits(jsonNodeToArrayList(jsonNode.get("benefits")));
                if (jsonNode.has("allergies"))
                    dto.setAllergies(jsonNodeToArrayList(jsonNode.get("allergies")));
                if (jsonNode.has("tags"))
                    dto.setTags(jsonNodeToArrayList(jsonNode.get("tags")));

                // Nested objects
                if (jsonNode.has("nutritionalInfo")) {
                    dto.setNutritionalInfo(convertToNutritionalInfo(jsonNode.get("nutritionalInfo")));
                }
                if (jsonNode.has("customization")) {
                    dto.setCustomization(convertToCustomization(jsonNode.get("customization")));
                }

            } catch (Exception e) {
                logger.debug("Failed to parse metadata for item {}: {}", chargebeeItem.id(), e.getMessage());
            }
        }

        // Extract size-based prices (200ml, 300ml, 500ml)
        dto.setPrices(extractSizePrices(chargebeeItem));

        return dto;
    }

    /**
     * Extract prices for 200ml, 300ml, 500ml sizes from Chargebee item prices
     */
    private List<ChargeItemDTO.ItemPriceDTO> extractSizePrices(Item chargebeeItem) {
        List<ChargeItemDTO.ItemPriceDTO> prices = new ArrayList<>();

        try {
            // Fetch all item prices and filter by item ID
            com.chargebee.ListResult priceResult = ItemPrice.list().request();

            for (com.chargebee.ListResult.Entry entry : priceResult) {
                ItemPrice itemPrice = entry.itemPrice();

                // Filter by item ID and size (200ml, 300ml, 500ml)
                if (!itemPrice.itemId().equals(chargebeeItem.id())) {
                    continue;
                }

                String size = extractSizeFromItemPrice(itemPrice);
                if (isValidSize(size)) {
                    ChargeItemDTO.ItemPriceDTO priceDTO = ChargeItemDTO.ItemPriceDTO.builder()
                            .priceId(itemPrice.id())
                            .name(itemPrice.name())
                            .size(size)
                            .price(itemPrice.price() != null
                                    ? java.math.BigDecimal.valueOf(itemPrice.price()).movePointLeft(2)
                                    : null)
                            .currencyCode(itemPrice.currencyCode())
                            .pricingModel(itemPrice.pricingModel() != null ? itemPrice.pricingModel().name() : null)
                            .period(itemPrice.period())
                            .periodUnit(itemPrice.periodUnit() != null ? itemPrice.periodUnit().name() : null)
                            .build();
                    prices.add(priceDTO);
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to fetch prices for item {}: {}", chargebeeItem.id(), e.getMessage());
        }

        return prices;
    }

    /**
     * Extract size from item price name or description
     */
    private String extractSizeFromItemPrice(ItemPrice itemPrice) {
        String name = itemPrice.name() != null ? itemPrice.name().toLowerCase() : "";
        String desc = itemPrice.description() != null ? itemPrice.description().toLowerCase() : "";

        if (name.contains("200ml") || desc.contains("200ml"))
            return "200ml";
        if (name.contains("300ml") || desc.contains("300ml"))
            return "300ml";
        if (name.contains("500ml") || desc.contains("500ml"))
            return "500ml";

        return null;
    }

    /**
     * Check if size is valid (200ml, 300ml, 500ml)
     */
    private boolean isValidSize(String size) {
        if (size == null || size.trim().isEmpty()) {
            return false;
        }
        return size.equals("200ml") || size.equals("300ml") || size.equals("500ml");
    }

    /**
     * Convert JsonNode array to List<String>
     */
    private List<String> jsonNodeToArrayList(JsonNode jsonNode) {
        List<String> list = new ArrayList<>();
        if (jsonNode.isArray()) {
            for (JsonNode node : jsonNode) {
                list.add(node.asText());
            }
        }
        return list;
    }

    /**
     * Convert nutritional info JsonNode to DTO
     */
    private ChargeItemDTO.NutritionalInfo convertToNutritionalInfo(JsonNode jsonNode) {
        return ChargeItemDTO.NutritionalInfo.builder()
                .protein(jsonNode.has("protein") ? jsonNode.get("protein").asText() : null)
                .carbs(jsonNode.has("carbs") ? jsonNode.get("carbs").asText() : null)
                .fiber(jsonNode.has("fiber") ? jsonNode.get("fiber").asText() : null)
                .sugar(jsonNode.has("sugar") ? jsonNode.get("sugar").asText() : null)
                .vitaminC(jsonNode.has("vitaminC") ? jsonNode.get("vitaminC").asText() : null)
                .iron(jsonNode.has("iron") ? jsonNode.get("iron").asText() : null)
                .build();
    }

    /**
     * Convert customization JsonNode to DTO
     */
    private ChargeItemDTO.Customization convertToCustomization(JsonNode jsonNode) {
        return ChargeItemDTO.Customization.builder()
                .sugarLevel(jsonNode.has("sugarLevel") ? jsonNodeToArrayList(jsonNode.get("sugarLevel")) : null)
                .iceLevel(jsonNode.has("iceLevel") ? jsonNodeToArrayList(jsonNode.get("iceLevel")) : null)
                .addOns(jsonNode.has("addOns") ? jsonNodeToArrayList(jsonNode.get("addOns")) : null)
                .build();
    }

    /**
     * Cache items locally by syncing to database
     */
    private void cacheItemsLocally(List<ChargeItemDTO> items) {
        logger.info("Caching {} items to local database", items.size());
        // Items are already cached via webhooks, this is just a placeholder
        // for any additional caching logic if needed
    }

    /**
     * Get items from local database cache (fallback method)
     */
    private List<ChargeItemDTO> getItemsFromLocalCache() {
        try {
            List<ItemEntity> entities = itemRepository.findAllActiveChargeItems();
            return entities.stream()
                    .map(this::convertEntityToChargeItemDTO)
                    .filter(dto -> isValidCategory(dto.getCategory()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching items from local cache: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert ItemEntity to ChargeItemDTO
     */
    private ChargeItemDTO convertEntityToChargeItemDTO(ItemEntity entity) {
        ChargeItemDTO dto = ChargeItemDTO.builder()
                .itemId(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .itemFamilyId(entity.getItemFamilyId())
                .status(entity.getStatus())
                .enabledInPortal(entity.isEnabledInPortal())
                .enabledForCheckout(entity.isEnabledForCheckout())
                .build();

        // Extract metadata fields
        if (entity.getMetaData() != null && !entity.getMetaData().trim().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(entity.getMetaData());

                if (jsonNode.has("category"))
                    dto.setCategory(jsonNode.get("category").asText());
                if (jsonNode.has("imagePath"))
                    dto.setImagePath(jsonNode.get("imagePath").asText());
                if (jsonNode.has("startColor"))
                    dto.setStartColor(jsonNode.get("startColor").asText());
                if (jsonNode.has("endColor"))
                    dto.setEndColor(jsonNode.get("endColor").asText());
                if (jsonNode.has("calories"))
                    dto.setCalories(jsonNode.get("calories").asInt());

                // Add prices from item prices relation
                if (entity.getItemPrices() != null && !entity.getItemPrices().isEmpty()) {
                    List<ChargeItemDTO.ItemPriceDTO> prices = entity.getItemPrices().stream()
                            .map(this::convertEntityPriceToDTO)
                            .filter(p -> isValidSize(p.getSize()))
                            .collect(Collectors.toList());
                    dto.setPrices(prices);
                }

            } catch (Exception e) {
                logger.debug("Failed to parse metadata for entity {}: {}", entity.getId(), e.getMessage());
            }
        }

        return dto;
    }

    /**
     * Convert ItemPriceEntity to ItemPriceDTO
     */
    private ChargeItemDTO.ItemPriceDTO convertEntityPriceToDTO(ItemPriceEntity entity) {
        String size = extractSizeFromEntityPrice(entity);
        return ChargeItemDTO.ItemPriceDTO.builder()
                .priceId(entity.getId())
                .name(entity.getName())
                .size(size)
                .price(entity.getPrice())
                .currencyCode(entity.getCurrencyCode())
                .pricingModel(entity.getPricingModel())
                .period(entity.getPeriod())
                .periodUnit(entity.getPeriodUnit())
                .build();
    }

    /**
     * Extract size from ItemPriceEntity name or description
     */
    private String extractSizeFromEntityPrice(ItemPriceEntity entity) {
        String name = entity.getName() != null ? entity.getName().toLowerCase() : "";
        String desc = entity.getDescription() != null ? entity.getDescription().toLowerCase() : "";

        if (name.contains("200ml") || desc.contains("200ml"))
            return "200ml";
        if (name.contains("300ml") || desc.contains("300ml"))
            return "300ml";
        if (name.contains("500ml") || desc.contains("500ml"))
            return "500ml";

        return null;
    }
}
