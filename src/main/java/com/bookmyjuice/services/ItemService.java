package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.ItemEntity;
import com.bookmyjuice.repository.ItemRepository;
import com.chargebee.models.Event;

@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    public ResponseEntity<?> saveItem(Event event) {
        if (itemRepository.existsById(event.content().item().id())) {
            return updateItem(event);
        } else {
            try {
                var item = event.content().item();
                // var sub = event.content().subscription(); // This line is not used, but it seems to be a placeholder for future use
                // var customerFromEvent = event.content().customer();
                ItemEntity entity = new ItemEntity();
                entity.setId(item.id());
                entity.setName(item.name());
                //metaData is not directly available in the item object, so it is commented out
                // entity.setMetaData(item.metadata().toString());
                entity.setDescription(item.description());
                entity.setType(item.type().name());
                entity.setStatus(item.status().name());
                entity.setExternalName(item.externalName());
                entity.setEnabledInPortal(item.enabledInPortal());
                entity.setEnabledForCheckout(item.enabledForCheckout());
                entity.setUnit(item.unit());
                entity.setItemFamilyId(item.itemFamilyId());
                itemRepository.save(entity);

                return ResponseEntity.ok("New item saved successfully");
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error saving item new item: " + e.getMessage());
            }
        }
    }

    public ResponseEntity<?> updateItem(Event event) {
        if (!itemRepository.existsById(event.content().item().id())) {
            return saveItem(event);
        } else {
            try {
                var item = event.content().item();
                ItemEntity entity = itemRepository.findById(item.id()).orElse(null);
                if (entity == null) {
                    // If item doesn't exist, create it
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
                    entity.setMetaData(item.metadata() != null ? item.metadata().toString() : null);
                    itemRepository.save(entity);
                    return ResponseEntity.ok("Item updated successfully");
                }
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error updating item: " + e.getMessage());
            }
        }
    }

    public ResponseEntity<?> deleteItem(Event event) {
        try {
            var item = event.content().item();
            itemRepository.deleteById(item.id());
            return ResponseEntity.ok("Item deleted successfully");
        } catch (Exception e) {
           return ResponseEntity.status(500).body("Error deleting item: " + e.getMessage());
        }
    }
}
