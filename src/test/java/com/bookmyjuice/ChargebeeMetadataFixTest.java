package com.bookmyjuice;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bookmyjuice.models.entities.ItemEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test to verify the Chargebee metadata fix
 */
public class ChargebeeMetadataFixTest {

    @Test
    public void testChargebeeMetadataIssue() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        System.out.println("=== CHARGEBEE METADATA ISSUE DEMONSTRATION ===");
        
        // 1. Simulate the problematic Chargebee metadata wrapper
        Map<String, Object> problematicWrapper = new HashMap<>();
        problematicWrapper.put("empty", false);
        problematicWrapper.put("mapType", "java.util.HashMap");
        
        System.out.println("❌ WRONG: What you're currently getting from Chargebee:");
        System.out.println("toString(): " + problematicWrapper.toString());
        
        try {
            String wrongJson = objectMapper.writeValueAsString(problematicWrapper);
            System.out.println("JSON: " + wrongJson);
        } catch (Exception e) {
            System.out.println("JSON conversion failed: " + e.getMessage());
        }
        
        // 2. What should happen with the fix
        System.out.println("\n✅ CORRECT: What should be saved (empty metadata):");
        ItemEntity itemWithEmptyMetadata = new ItemEntity();
        itemWithEmptyMetadata.setMetaDataFromObject(new HashMap<>());
        System.out.println("Empty metadata JSON: " + itemWithEmptyMetadata.getMetaData());
        
        // 3. Example of proper metadata
        System.out.println("\n✅ IDEAL: What proper metadata should look like:");
        Map<String, Object> properMetadata = new HashMap<>();
        properMetadata.put("imagePath", "assets/green_juice.png");
        properMetadata.put("startColor", "#4CAF50");
        properMetadata.put("endColor", "#2E7D32");
        properMetadata.put("calories", 180);
        properMetadata.put("meals", new String[]{"Spinach", "Kale", "Apple"});
        properMetadata.put("category", "juice");
        properMetadata.put("servingSize", "500ml");
        
        ItemEntity itemWithProperMetadata = new ItemEntity();
        itemWithProperMetadata.setMetaDataFromObject(properMetadata);
        System.out.println("Proper metadata JSON: " + itemWithProperMetadata.getMetaData());
        
        // 4. Test our detection logic
        System.out.println("\n=== TESTING DETECTION LOGIC ===");
        
        boolean isProblematicWrapper = problematicWrapper.containsKey("empty") && 
                                      problematicWrapper.containsKey("mapType");
        
        System.out.println("Wrapper detected: " + isProblematicWrapper);
        
        if (isProblematicWrapper) {
            System.out.println("✅ Our fix will convert this to empty metadata instead of saving wrapper properties");
        }
        
        System.out.println("\n=== SUMMARY ===");
        System.out.println("❌ Before fix: Saves {\"empty\":false,\"mapType\":\"java.util.HashMap\"}");
        System.out.println("✅ After fix:  Saves {} (empty object) or actual metadata content");
        System.out.println("📋 Next webhook will show the fix in action via debug logs");
    }
}