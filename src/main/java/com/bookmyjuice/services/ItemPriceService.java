package com.bookmyjuice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.entities.ItemEntity;
import com.bookmyjuice.models.entities.ItemPriceEntity;
import com.bookmyjuice.repository.ItemPriceRepository;
import com.chargebee.models.Event;

@Service
public class ItemPriceService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItemPriceService.class);
    
    @Autowired
    private ItemPriceRepository itemPriceRepository;

    @Autowired
    private com.bookmyjuice.repository.ItemRepository itemRepository;

    public boolean saveItemPrice(Event event) {
        var itemPrice = event.content().itemPrice();
        logger.info("Processing save ItemPrice event for ID: {}", itemPrice.id());
        
        // Ensure parent Item exists or create it
        ItemEntity itemEntity = ensureParentItemExists(itemPrice.itemId(), event);
        
        if (itemPriceRepository.existsById(itemPrice.id())) {
            logger.info("ItemPrice already exists with ID: {}, updating instead", itemPrice.id());
            return updateItemPrice(event);
        }
        try {
            ItemPriceEntity entity = new ItemPriceEntity();
            populateItemPriceEntity(entity, itemPrice, itemEntity);
            itemPriceRepository.save(entity);
            logger.info("New ItemPrice saved successfully with ID: {}", itemPrice.id());
            return true;
        } catch (Exception e) {
            logger.error("Error saving ItemPrice {}: {}", itemPrice.id(), e.getMessage(), e);
            return false;
        }
    }

    public boolean updateItemPrice(Event event) {
        var itemPrice = event.content().itemPrice();
        logger.info("Processing update ItemPrice event for ID: {}", itemPrice.id());
        
        // Ensure parent Item exists or create it
        ItemEntity itemEntity = ensureParentItemExists(itemPrice.itemId(), event);
        
        try {
            ItemPriceEntity entity = itemPriceRepository.findById(itemPrice.id()).orElse(null);
            if (entity == null) {
                logger.warn("ItemPrice not found with ID: {}, creating new one instead", itemPrice.id());
                return saveItemPriceInternal(event, itemEntity);
            }
            
            populateItemPriceEntity(entity, itemPrice, itemEntity);
            itemPriceRepository.save(entity);
            logger.info("ItemPrice updated successfully with ID: {}", itemPrice.id());
            return true;
        } catch (Exception e) {
            logger.error("Error updating ItemPrice {}: {}", itemPrice.id(), e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteItemPrice(Event event) {
        var itemPrice = event.content().itemPrice();
        logger.info("Processing delete ItemPrice event for ID: {}", itemPrice.id());
        
        try {
            if (itemPriceRepository.existsById(itemPrice.id())) {
                itemPriceRepository.deleteById(itemPrice.id());
                logger.info("ItemPrice deleted successfully with ID: {}", itemPrice.id());
                return true;
            } else {
                logger.warn("ItemPrice not found with ID: {}", itemPrice.id());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error deleting ItemPrice {}: {}", itemPrice.id(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Save or update ItemPrice - used for nested ItemPrice processing
     */
    public boolean saveOrUpdateItemPrice(Event event) {
        var itemPrice = event.content().itemPrice();
        if (itemPriceRepository.existsById(itemPrice.id())) {
            return updateItemPrice(event);
        } else {
            return saveItemPrice(event);
        }
    }
    
    /**
     * Internal method to save ItemPrice with already created/fetched ItemEntity
     * This avoids duplicate parent item creation
     */
    private boolean saveItemPriceInternal(Event event, ItemEntity itemEntity) {
        var itemPrice = event.content().itemPrice();
        logger.info("Processing save ItemPrice event (internal) for ID: {}", itemPrice.id());
        
        try {
            ItemPriceEntity entity = new ItemPriceEntity();
            populateItemPriceEntity(entity, itemPrice, itemEntity);
            itemPriceRepository.save(entity);
            logger.info("New ItemPrice saved successfully (internal) with ID: {}", itemPrice.id());
            return true;
        } catch (Exception e) {
            logger.error("Error saving ItemPrice (internal) {}: {}", itemPrice.id(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Ensure parent Item exists or create a minimal one from available data
     */
    private ItemEntity ensureParentItemExists(String itemId, Event event) {
        if (itemId == null) {
            logger.warn("No itemId provided for ItemPrice");
            return null;
        }
        
        ItemEntity itemEntity = itemRepository.findById(itemId).orElse(null);
        if (itemEntity == null) {
            logger.info("Parent Item {} not found, creating minimal Item entity", itemId);
            itemEntity = new ItemEntity();
            itemEntity.setId(itemId);
            
            // Try to get additional Item data from the event if available
            if (event.content() != null && event.content().item() != null) {
                var item = event.content().item();
                logger.info("Found Item data in event, populating minimal fields");
                itemEntity.setName(item.name());
                itemEntity.setDescription(item.description());
                itemEntity.setType(item.type() != null ? item.type().name() : "charge");
                itemEntity.setStatus(item.status() != null ? item.status().name() : "active");
                itemEntity.setEnabledInPortal(item.enabledInPortal() != null ? item.enabledInPortal() : true);
                itemEntity.setEnabledForCheckout(item.enabledForCheckout() != null ? item.enabledForCheckout() : true);
            } else {
                // Set minimal default values
                logger.info("No Item data in event, setting minimal defaults");
                itemEntity.setName("Item " + itemId);
                itemEntity.setType("charge");
                itemEntity.setStatus("active");
                itemEntity.setEnabledInPortal(true);
                itemEntity.setEnabledForCheckout(true);
            }
            
            itemRepository.save(itemEntity);
            logger.info("Created minimal Item entity with ID: {}", itemId);
        }
        
        return itemEntity;
    }
    
    /**
     * Populate ItemPriceEntity with data from Chargebee ItemPrice
     */
    private void populateItemPriceEntity(ItemPriceEntity entity, com.chargebee.models.ItemPrice itemPrice, ItemEntity itemEntity) {
        entity.setId(itemPrice.id());
        entity.setItem(itemEntity); // Link via JPA relation
        entity.setName(itemPrice.name());
        entity.setDescription(itemPrice.description());
        entity.setExternalName(itemPrice.externalName());
        entity.setPricingModel(itemPrice.pricingModel() != null ? itemPrice.pricingModel().toString() : null);
        entity.setPrice(itemPrice.price() != null ? java.math.BigDecimal.valueOf(itemPrice.price()).movePointLeft(2) : null);
        entity.setCurrencyCode(itemPrice.currencyCode());
        entity.setStatus(itemPrice.status() != null ? itemPrice.status().toString() : null);
        entity.setPeriodUnit(itemPrice.periodUnit() != null ? itemPrice.periodUnit().toString() : null);
        entity.setPeriod(itemPrice.period());
        entity.setTrialPeriod(itemPrice.trialPeriod());
        entity.setTrialPeriodUnit(itemPrice.trialPeriodUnit() != null ? itemPrice.trialPeriodUnit().toString() : null);
        entity.setInvoiceNotes(itemPrice.invoiceNotes());
        entity.setCreatedAt(itemPrice.createdAt() != null ? itemPrice.createdAt().toLocalDateTime() : null);
        entity.setUpdatedAt(itemPrice.updatedAt() != null ? itemPrice.updatedAt().toLocalDateTime() : null);
        
        // Set additional fields if available
        entity.setTrialAvailable(itemPrice.trialPeriod() != null && itemPrice.trialPeriod() > 0);
        entity.setFreeQuantityInDecimal(false); // Default value
        
        logger.debug("Populated ItemPriceEntity {} with data from Chargebee", itemPrice.id());
    }
}
