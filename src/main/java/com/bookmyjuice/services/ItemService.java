package com.bookmyjuice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.entities.ItemEntity;
import com.bookmyjuice.repository.ItemRepository;
import com.chargebee.models.Event;

@Service
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemRepository itemRepository;

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
                entity.setMetaData(item.metadata() != null ? item.metadata().toString() : null);

                itemRepository.save(entity);
                logger.info("New item saved successfully with ID: {}", item.id());
                return ResponseEntity.ok("New item saved successfully");
            }
        } catch (Exception e) {
            logger.error("Error occurred while saving item: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error saving item: " + e.getMessage());
        }
    }

    public ResponseEntity<?> updateItem(Event event) {
        if (event == null || event.content() == null || event.content().item() == null) {
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
                    entity.setMetaData(item.metadata() != null ? item.metadata().toString() : null);

                    itemRepository.save(entity);
                    logger.info("Item updated successfully with ID: {}", item.id());
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
}