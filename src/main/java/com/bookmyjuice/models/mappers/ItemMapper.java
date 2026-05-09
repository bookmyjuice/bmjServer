package com.bookmyjuice.models.mappers;

import java.util.HashMap;
import java.util.Map;

import com.bookmyjuice.models.entities.ItemEntity;
import com.chargebee.models.Item;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ItemMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Convert Item to ItemEntity
    public static ItemEntity toEntity(Item item) {
        if (item.id() == null || item.id().isEmpty()) {
            throw new IllegalArgumentException("Item ID is missing");
        }
        ItemEntity entity = new ItemEntity();
        entity.setId(item.id());
        entity.setName(item.name());
        entity.setDescription(item.description());
        entity.setType(item.type() != null ? item.type().toString() : null);
        entity.setStatus(item.status() != null ? item.status().toString() : null);
        entity.setExternalName(item.externalName());
        entity.setEnabledInPortal(item.enabledInPortal() != null ? item.enabledInPortal() : false);
        entity.setEnabledForCheckout(item.enabledForCheckout() != null ? item.enabledForCheckout() : false);
        entity.setProductFamilyId(item.itemFamilyId());
        entity.setUnit(item.unit());
        // Set default values for fields that may not be available in all Chargebee versions
        entity.setArchived(false); // Default to not archived
        entity.setGiftable(false); // Default to not giftable
        entity.setShippable(false); // Default to not shippable
        entity.setDeleted(false); // New items are not deleted
        
        // Handle metadata - always use setMetaDataFromObject to ensure JSON
        // Handle metadata from Chargebee
                    if (item.metadata() != null) {
                        try {
                                                       Object metadataObj = item.metadata();
                            Map<String, Object> actualMetadata = convertChargebeeMetadata(metadataObj);

                            boolean onlyWrapper = actualMetadata != null && actualMetadata.size() == 2 && actualMetadata.containsKey("mapType") && actualMetadata.containsKey("empty");
                            if (onlyWrapper) {
                                entity.setMetaData("{}");
                                // logger.info("Updating item {} with blank metadata (wrapper only)", item.id());
                            } else if (actualMetadata != null && !actualMetadata.isEmpty()) {
                                entity.setMetaDataFromObject(actualMetadata);
                                // logger.info("Updating item {} with converted metadata: {}", item.id(), entity.getMetaData());
                            } else {
                                entity.setMetaData(null);
                                // logger.warn("No valid metadata found for item {}", item.id());
                            }
                        } catch (Exception e) {
                            // logger.error("Failed to convert metadata for item {}: {}", item.id(), e.getMessage(), e);
                            entity.setMetaData(null);
                        }
                    } else {
                        entity.setMetaData(null);
                        // logger.info("Updating item {} with no metadata", item.id());
                    }
        
        // Store the complete JSON object for future reference
        try {
            entity.setJsonObject(objectMapper.writeValueAsString(item));
        } catch (Exception e) {
            System.err.println("Failed to serialize complete item object for " + item.id() + ": " + e.getMessage());
        }
        
        return entity;
    }

    private static Map<String, Object> convertChargebeeMetadata(Object metadataObj) {
        if (metadataObj == null) {
            // logger.info("Metadata object is null");
            return new HashMap<>();
        }

        String className = metadataObj.getClass().getName();
        // logger.info("Converting Chargebee metadata of type: {}", className);

        // Special handling for Chargebee JSONObject
        if ("com.chargebee.org.json.JSONObject".equals(className)) {
            // logger.info("Detected Chargebee JSONObject, using toString() method");
            try {
                String jsonString = metadataObj.toString();
                // logger.info("JSONObject toString result: {}", jsonString);
                @SuppressWarnings("unchecked")
                Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
                // logger.info("Successfully parsed JSONObject toString to Map with {} entries", map.size());
                return map;
            } catch (Exception e) {
                // logger.error("Failed to parse Chargebee JSONObject toString: {}", e.getMessage());
                return new HashMap<>();
            }
        }

        // If it's already a Map, check for wrapper and extract real data if present
        if (metadataObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) metadataObj;
            // logger.info("Metadata is a Map with {} entries", map.size());

            // If it has mapType/empty but also other fields, extract only real fields
            Map<String, Object> cleanedMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                if (!key.equals("empty") && !key.equals("mapType")) {
                    cleanedMap.put(key, entry.getValue());
                }
            }
            if (!cleanedMap.isEmpty()) {
                // logger.info("Cleaned map has {} entries (real data extracted)", cleanedMap.size());
                return cleanedMap;
            } else if (map.containsKey("empty") && map.containsKey("mapType")) {
                // logger.warn("Detected Chargebee metadata wrapper with ONLY empty/mapType properties");
                // logger.warn("Wrapper content: empty={}, mapType={}", map.get("empty"), map.get("mapType"));
                return new HashMap<>();
            } else {
                // logger.info("Map has no real data fields, returning as is");
                return map;
            }
        }
        
        // If it's not a Map or JSONObject, try to convert it using ObjectMapper
        try {
            // logger.info("Attempting ObjectMapper conversion for type: {}", className);
            String jsonString = objectMapper.writeValueAsString(metadataObj);
            // logger.info("ObjectMapper converted metadata to: {}", jsonString);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(jsonString, Map.class);
            
            // Check if the converted map is the problematic wrapper
            if (map.containsKey("empty") && map.containsKey("mapType") && map.size() <= 2) {
                // logger.warn("ObjectMapper conversion resulted in wrapper properties only");
                return new HashMap<>();
            }
            
            // logger.info("ObjectMapper conversion successful, returning map with {} entries", map.size());
            return map;
            
        } catch (Exception e) {
            // logger.error("Failed to convert metadata object using ObjectMapper: {}", e.getMessage());
            return new HashMap<>();
        }
    }

}
