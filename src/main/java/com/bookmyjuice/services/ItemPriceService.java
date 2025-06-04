package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.entities.ItemPriceEntity;
import com.bookmyjuice.repository.ItemPriceRepository;
import com.chargebee.models.Event;

@Service
public class ItemPriceService {
    @Autowired
    private ItemPriceRepository itemPriceRepository;

    @Autowired
    private com.bookmyjuice.repository.ItemRepository itemRepository;

    public boolean saveItemPrice(Event event) {
        var itemPrice = event.content().itemPrice();
        // Ensure parent Item exists or create it
        com.bookmyjuice.models.entities.ItemEntity itemEntity = null;
        if (itemPrice.itemId() != null) {
            itemEntity = itemRepository.findById(itemPrice.itemId()).orElse(null);
            if (itemEntity == null) {
                itemEntity = new com.bookmyjuice.models.entities.ItemEntity();
                itemEntity.setId(itemPrice.itemId());
                // Set more fields if available from itemPrice
                itemRepository.save(itemEntity);
            }
        }
        ItemPriceEntity entity = new ItemPriceEntity();
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
        // Only map fields that exist in the SDK
        itemPriceRepository.save(entity);
        return true;
    }

    public boolean updateItemPrice(Event event) {
        var itemPrice = event.content().itemPrice();
        // Ensure parent Item exists or create it
        com.bookmyjuice.models.entities.ItemEntity itemEntity = null;
        if (itemPrice.itemId() != null) {
            itemEntity = itemRepository.findById(itemPrice.itemId()).orElse(null);
            if (itemEntity == null) {
                itemEntity = new com.bookmyjuice.models.entities.ItemEntity();
                itemEntity.setId(itemPrice.itemId());
                // Set more fields if available from itemPrice
                itemRepository.save(itemEntity);
            }
        }
        ItemPriceEntity entity = itemPriceRepository.findById(itemPrice.id()).orElse(new ItemPriceEntity());
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
        // Only map fields that exist in the SDK
        itemPriceRepository.save(entity);
        return true;
    }

    public boolean deleteItemPrice(Event event) {
        var itemPrice = event.content().itemPrice();
        itemPriceRepository.deleteById(itemPrice.id());
        return true;
    }
}
