package com.bookmyjuice;

import com.bookmyjuice.models.entities.ItemEntity;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class JsonMetadataTest {

    @Test
    public void testJsonMetadataFunctionality() {
        // Create a sample item
        ItemEntity item = new ItemEntity();
        item.setId("test_juice_001");
        item.setName("Test Juice");
        item.setDescription("A test juice for JSON metadata");

        // Create sample metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("imagePath", "assets/test_juice.png");
        metadata.put("startColor", "#FF5722");
        metadata.put("endColor", "#D32F2F");
        metadata.put("calories", 150);
        metadata.put("meals", new String[]{"Orange", "Carrot", "Ginger"});
        metadata.put("category", "juice");
        metadata.put("servingSize", "350ml");
        metadata.put("organic", true);
        metadata.put("popularity", 75);

        // Set metadata using the utility method
        item.setMetaDataFromObject(metadata);

        // Print the JSON string
        System.out.println("Generated JSON Metadata:");
        System.out.println(item.getMetaData());

        // Test reading individual fields
        System.out.println("\n=== Reading Individual Fields ===");
        System.out.println("Image Path: " + item.getMetaDataField("imagePath"));
        System.out.println("Start Color: " + item.getMetaDataField("startColor"));
        System.out.println("Calories: " + item.getMetaDataFieldAsInt("calories"));
        System.out.println("Serving Size: " + item.getMetaDataField("servingSize"));
        System.out.println("Popularity: " + item.getMetaDataFieldAsInt("popularity"));

        // Test reading array fields
        System.out.println("\nIngredients:");
        String[] meals = item.getMetaDataFieldAsArray("meals");
        for (String meal : meals) {
            System.out.println("  - " + meal);
        }

        // Test with actual JSON you'd save to Chargebee
        System.out.println("\n=== Sample JSON for Chargebee Item ===");
        Map<String, Object> chargebeeMetadata = new HashMap<>();
        chargebeeMetadata.put("imagePath", "assets/mango_passion.png");
        chargebeeMetadata.put("startColor", "#FFC107");
        chargebeeMetadata.put("endColor", "#FF8F00");
        chargebeeMetadata.put("calories", 200);
        chargebeeMetadata.put("meals", new String[]{"Mango", "Passion Fruit", "Lime"});
        chargebeeMetadata.put("category", "juice");
        chargebeeMetadata.put("benefits", new String[]{"Vitamin C", "Antioxidants", "Energy"});
        chargebeeMetadata.put("shelfLife", "2 days");
        chargebeeMetadata.put("servingSize", "500ml");

        ItemEntity chargebeeItem = new ItemEntity();
        chargebeeItem.setMetaDataFromObject(chargebeeMetadata);
        
        System.out.println("Chargebee Item Metadata JSON:");
        System.out.println(chargebeeItem.getMetaData());
    }
}