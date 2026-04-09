package com.bookmyjuice;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bookmyjuice.models.entities.ItemEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MetadataDebugTest {

    @Test
    public void testMetadataConversion() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Simulate what might be coming from Chargebee
        Map<String, Object> chargebeeMetadata = new HashMap<>();
        chargebeeMetadata.put("imagePath", "assets/juice.png");
        chargebeeMetadata.put("startColor", "#FF5722");
        chargebeeMetadata.put("calories", 150);
        chargebeeMetadata.put("meals", new String[]{"Orange", "Carrot"});

        System.out.println("=== DEBUGGING METADATA CONVERSION ===");
        System.out.println("Original metadata class: " + chargebeeMetadata.getClass().getName());
        System.out.println("Original metadata toString(): " + chargebeeMetadata.toString());

        // Test 1: Direct ObjectMapper conversion
        try {
            String jsonString = objectMapper.writeValueAsString(chargebeeMetadata);
            System.out.println("Direct ObjectMapper result: " + jsonString);
        } catch (Exception e) {
            System.out.println("Direct ObjectMapper failed: " + e.getMessage());
        }

        // Test 2: Using ItemEntity utility method
        ItemEntity item = new ItemEntity();
        try {
            item.setMetaDataFromObject(chargebeeMetadata);
            System.out.println("ItemEntity utility method result: " + item.getMetaData());
        } catch (Exception e) {
            System.out.println("ItemEntity utility method failed: " + e.getMessage());
        }

        // Test 3: What happens if we get toString() result
        String toStringResult = chargebeeMetadata.toString();
        System.out.println("toString() result: " + toStringResult);

        // Test 4: Check if it's the format you're seeing
        if (toStringResult.contains("empty") && toStringResult.contains("mapType")) {
            System.out.println("⚠️  This looks like the problematic format!");
        } else {
            System.out.println("✓ This is normal HashMap toString()");
        }

        // Test 5: Create a metadata object that would produce the problematic output
        Map<String, Object> problematicMetadata = new HashMap<>();
        problematicMetadata.put("empty", false);
        problematicMetadata.put("mapType", "java.util.HashMap");
        
        System.out.println("\n=== TESTING PROBLEMATIC METADATA ===");
        System.out.println("Problematic metadata toString(): " + problematicMetadata.toString());
        
        try {
            String jsonString = objectMapper.writeValueAsString(problematicMetadata);
            System.out.println("Problematic metadata as JSON: " + jsonString);
        } catch (Exception e) {
            System.out.println("Failed to convert problematic metadata: " + e.getMessage());
        }
    }
}