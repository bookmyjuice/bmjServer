package com.bookmyjuice.services;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.entities.ItemEntity;
import com.bookmyjuice.repository.ItemRepository;

@Service
public class MetadataTestService {

    @Autowired
    private ItemRepository itemRepository;

    /**
     * Example: Create a juice item with rich JSON metadata
     */
    public ItemEntity createJuiceWithMetadata() {
        // Create the metadata object
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("imagePath", "assets/green_detox.png");
        metadata.put("startColor", "#4CAF50");
        metadata.put("endColor", "#2E7D32");
        metadata.put("calories", 180);
        metadata.put("meals", new String[]{"Spinach", "Kale", "Cucumber", "Green Apple", "Lemon", "Ginger"});
        metadata.put("category", "juice");
        metadata.put("subcategory", "green_juice");
        
        // Nutritional info as nested object
        Map<String, String> nutritionalInfo = new HashMap<>();
        nutritionalInfo.put("protein", "3g");
        nutritionalInfo.put("carbs", "42g");
        nutritionalInfo.put("fiber", "2g");
        nutritionalInfo.put("sugar", "36g");
        nutritionalInfo.put("vitaminC", "120mg");
        nutritionalInfo.put("iron", "2mg");
        metadata.put("nutritionalInfo", nutritionalInfo);
        
        metadata.put("benefits", new String[]{"Detoxification", "Immune boost", "Energy enhancement", "Digestive health"});
        metadata.put("allergies", new String[]{"None"});
        metadata.put("shelfLife", "3 days");
        metadata.put("preparationTime", "10 minutes");
        metadata.put("servingSize", "500ml");
        metadata.put("temperature", "cold");
        metadata.put("popularity", 85);
        metadata.put("seasonal", false);
        metadata.put("tags", new String[]{"detox", "green", "healthy", "organic", "cold-pressed"});
        
        // Customization options
        Map<String, Object> customization = new HashMap<>();
        customization.put("sugarLevel", new String[]{"no-sugar", "low-sugar", "regular"});
        customization.put("iceLevel", new String[]{"no-ice", "less-ice", "regular-ice"});
        customization.put("addOns", new String[]{"chia-seeds", "protein-powder", "honey"});
        metadata.put("customization", customization);

        // Create the item entity
        ItemEntity item = new ItemEntity();
        item.setId("green_detox_001");
        item.setName("Green Detox Juice");
        item.setDescription("A refreshing blend of green vegetables and fruits for ultimate detoxification");
        item.setType("juice");
        item.setStatus("active");
        item.setExternalName("Green Detox Premium");
        item.setEnabledInPortal(true);
        item.setEnabledForCheckout(true);
        item.setProductFamilyId("juice_family");
        item.setUnit("ml");
        item.setArchived(false);

        // Set metadata using the utility method
        item.setMetaDataFromObject(metadata);

        // Save to database
        return itemRepository.save(item);
    }

    /**
     * Example: Read and display metadata from an existing item
     */
    public ItemEntity displayItemMetadata(String itemId) {
        ItemEntity item = itemRepository.findById(itemId).orElse(null);
        if (item == null) {
            System.out.println("Item not found: " + itemId);
            return null;
        }

        System.out.println("=== ITEM METADATA ===");
        System.out.println("Item ID: " + item.getId());
        System.out.println("Name: " + item.getName());
        
        // Using utility methods to get metadata fields
        System.out.println("Image Path: " + item.getMetaDataField("imagePath"));
        System.out.println("Start Color: " + item.getMetaDataField("startColor"));
        System.out.println("End Color: " + item.getMetaDataField("endColor"));
        System.out.println("Calories: " + item.getMetaDataFieldAsInt("calories"));
        System.out.println("Serving Size: " + item.getMetaDataField("servingSize"));
        System.out.println("Category: " + item.getMetaDataField("category"));
        
        // Get meals as array
        String[] meals = item.getMetaDataFieldAsArray("meals");
        System.out.println("Ingredients: ");
        for (String meal : meals) {
            System.out.println("  - " + meal);
        }
        
        // Get benefits as array
        String[] benefits = item.getMetaDataFieldAsArray("benefits");
        System.out.println("Benefits: ");
        for (String benefit : benefits) {
            System.out.println("  - " + benefit);
        }

        // Raw JSON metadata
        System.out.println("\n=== RAW JSON METADATA ===");
        System.out.println(item.getMetaData());
        
        return item;
    }

    /**
     * Example: Update specific metadata fields
     */
    public ItemEntity updateItemMetadata(String itemId, String newImagePath, Integer newCalories) {
        ItemEntity item = itemRepository.findById(itemId).orElse(null);
        if (item == null) {
            return null;
        }

        // Get existing metadata as JsonNode
        var metadataJson = item.getMetaDataAsJson();
        
        // Convert to Map for easier manipulation
        Map<String, Object> metadata = new HashMap<>();
        metadataJson.fields().forEachRemaining(entry -> {
            if (entry.getValue().isTextual()) {
                metadata.put(entry.getKey(), entry.getValue().asText());
            } else if (entry.getValue().isInt()) {
                metadata.put(entry.getKey(), entry.getValue().asInt());
            } else if (entry.getValue().isArray()) {
                String[] array = new String[entry.getValue().size()];
                for (int i = 0; i < entry.getValue().size(); i++) {
                    array[i] = entry.getValue().get(i).asText();
                }
                metadata.put(entry.getKey(), array);
            }
            // Add more type handling as needed
        });

        // Update specific fields
        if (newImagePath != null) {
            metadata.put("imagePath", newImagePath);
        }
        if (newCalories != null) {
            metadata.put("calories", newCalories);
        }

        // Save updated metadata
        item.setMetaDataFromObject(metadata);
        return itemRepository.save(item);
    }
}
