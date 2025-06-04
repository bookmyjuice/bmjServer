package com.bookmyjuice.models.mappers;

import com.bookmyjuice.models.entities.ItemEntity;
import com.chargebee.models.Item;

public class ItemMapper {

    // Convert Item to ItemEntity
    public static ItemEntity toEntity(Item item) {
        if (item.id() == null || item.id().isEmpty()) {
            throw new IllegalArgumentException("Item ID is missing");
        }
        ItemEntity entity = new ItemEntity();
        entity.setId(item.id());
        entity.setName(item.name());
        entity.setDescription(item.description());
        entity.setType(item.type().toString());
        entity.setStatus(item.status().toString());
        entity.setExternalName(item.externalName());
        entity.setEnabledInPortal(item.enabledInPortal());
        entity.setEnabledForCheckout(item.enabledForCheckout());
        entity.setItemFamilyId(item.itemFamilyId());
        entity.setUnit(item.unit() != null ? item.unit() : null);
        return entity;
    }
}