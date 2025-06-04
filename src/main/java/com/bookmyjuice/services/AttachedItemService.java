package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bookmyjuice.models.entities.AttachedItemEntity;
import com.bookmyjuice.repository.AttachedItemRepository;
import com.bookmyjuice.models.entities.AddonEntity;
import com.bookmyjuice.repository.AddonRepository;
import com.chargebee.models.Event;

@Service
public class AttachedItemService {
    @Autowired
    private AttachedItemRepository attachedItemRepository;
    @Autowired
    private AddonRepository addonRepository;

    public boolean saveAttachedItem(Event event) {
        var attachedItem = event.content().attachedItem();
        AttachedItemEntity entity = new AttachedItemEntity();
        entity.setId(attachedItem.id());
        entity.setParentItemId(attachedItem.parentItemId());
        // attachedItemId is not available in SDK, use attachedItem.itemId() if present
        entity.setAttachedItemId(attachedItem.itemId() != null ? attachedItem.itemId() : null);
        entity.setType(attachedItem.type() != null ? attachedItem.type().toString() : null);
        entity.setStatus(attachedItem.status() != null ? attachedItem.status().toString() : null);
        entity.setBillingCycles(attachedItem.billingCycles() != null ? attachedItem.billingCycles().toString() : null);
        entity.setQuantity(attachedItem.quantity() != null ? attachedItem.quantity().toString() : null);
        entity.setQuantityInDecimal(attachedItem.quantityInDecimal());
        // billingAlignmentMode is not available in SDK, skip or set null
        entity.setBillingAlignmentMode(null);
        entity.setChannel(attachedItem.channel() != null ? attachedItem.channel().toString() : null);
        // Link to AddonEntity if parentItemId matches an addon
        AddonEntity addon = addonRepository.findById(attachedItem.parentItemId()).orElse(null);
        entity.setAddon(addon);
        attachedItemRepository.save(entity);
        return true;
    }

    public boolean updateAttachedItem(Event event) {
        var attachedItem = event.content().attachedItem();
        AttachedItemEntity entity = attachedItemRepository.findById(attachedItem.id()).orElse(new AttachedItemEntity());
        entity.setId(attachedItem.id());
        entity.setParentItemId(attachedItem.parentItemId());
        entity.setAttachedItemId(attachedItem.itemId() != null ? attachedItem.itemId() : null);
        entity.setType(attachedItem.type() != null ? attachedItem.type().toString() : null);
        entity.setStatus(attachedItem.status() != null ? attachedItem.status().toString() : null);
        entity.setBillingCycles(attachedItem.billingCycles() != null ? attachedItem.billingCycles().toString() : null);
        entity.setQuantity(attachedItem.quantity() != null ? attachedItem.quantity().toString() : null);
        entity.setQuantityInDecimal(attachedItem.quantityInDecimal());
        entity.setBillingAlignmentMode(null);
        entity.setChannel(attachedItem.channel() != null ? attachedItem.channel().toString() : null);
        AddonEntity addon = addonRepository.findById(attachedItem.parentItemId()).orElse(null);
        entity.setAddon(addon);
        attachedItemRepository.save(entity);
        return true;
    }

    public boolean deleteAttachedItem(Event event) {
        var attachedItem = event.content().attachedItem();
        attachedItemRepository.deleteById(attachedItem.id());
        return true;
    }
}
