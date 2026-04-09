package com.bookmyjuice;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test to simulate the exact Chargebee JSONObject issue
 */
public class ChargebeeJSONObjectTest {

    @Test
    public void testChargebeeJSONObjectIssue() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        System.out.println("=== SIMULATING CHARGEBEE JSONOBJECT ISSUE ===");
        
        // Create a mock object that behaves like Chargebee JSONObject
        MockChargebeeJSONObject mockJSONObject = new MockChargebeeJSONObject();
        
        System.out.println("Mock object class: " + mockJSONObject.getClass().getName());
        System.out.println("Mock object toString(): " + mockJSONObject.toString());
        
        // Test what Jackson ObjectMapper does to it
        try {
            String jacksonResult = objectMapper.writeValueAsString(mockJSONObject);
            System.out.println("Jackson ObjectMapper result: " + jacksonResult);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> parsedBack = objectMapper.readValue(jacksonResult, Map.class);
            System.out.println("Parsed back map: " + parsedBack);
            
        } catch (Exception e) {
            System.out.println("Jackson conversion failed: " + e.getMessage());
        }
        
        // Test our fix approach
        System.out.println("\n=== TESTING OUR FIX ===");
        
        // What our fix should do
        String directToString = mockJSONObject.toString();
        System.out.println("Direct toString(): " + directToString);
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> properMap = objectMapper.readValue(directToString, Map.class);
            System.out.println("✅ Fix result: Successfully parsed toString() to proper Map");
            System.out.println("✅ Map has " + properMap.size() + " properties");
            System.out.println("✅ Sample properties: imagePath=" + properMap.get("imagePath") + 
                             ", calories=" + properMap.get("calories"));
            
        } catch (Exception e) {
            System.out.println("❌ Fix failed: " + e.getMessage());
        }
        
        System.out.println("\n=== SUMMARY ===");
        System.out.println("❌ Problem: Jackson serializes Chargebee JSONObject to wrapper properties");
        System.out.println("✅ Solution: Use JSONObject.toString() directly, then parse that JSON");
    }
    
    /**
     * Mock class to simulate com.chargebee.org.json.JSONObject behavior
     */
    private static class MockChargebeeJSONObject {
        
        @Override
        public String toString() {
            // This is what Chargebee JSONObject toString() returns - real JSON
            return "{\"benefits\":[\"Detoxification\",\"Immune boost\",\"Energy enhancement\",\"Digestive health\"],\"allergies\":[\"None\"],\"nutritionalInfo\":{\"fiber\":\"2g\",\"vitaminC\":\"120mg\",\"carbs\":\"42g\",\"protein\":\"3g\",\"iron\":\"2mg\",\"sugar\":\"36g\"},\"endColor\":\"#2E7D32\",\"seasonal\":false,\"customization\":{\"iceLevel\":[\"no-ice\",\"less-ice\",\"regular-ice\"],\"addOns\":[\"chia-seeds\",\"protein-powder\",\"honey\"],\"sugarLevel\":[\"no-sugar\",\"low-sugar\",\"regular\"]},\"imagePath\":\"assets/green_detox.png\",\"preparationTime\":\"10 minutes\",\"calories\":180,\"startColor\":\"#4CAF50\",\"tags\":[\"detox\",\"green\",\"healthy\",\"organic\",\"cold-pressed\"],\"popularity\":85,\"temperature\":\"cold\",\"category\":\"juice\",\"subcategory\":\"green_juice\",\"shelfLife\":\"3 days\",\"servingSize\":\"500ml\",\"meals\":[\"Spinach\",\"Kale\",\"Cucumber\",\"Green Apple\",\"Lemon\",\"Ginger\"]}";
        }
        
        // These properties cause Jackson to serialize it as wrapper
        public boolean isEmpty() {
            return false;
        }
        
        public String getMapType() {
            return "java.util.HashMap";
        }
    }
}