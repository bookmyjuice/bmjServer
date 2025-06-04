package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.entities.AddonEntity;
import com.bookmyjuice.repository.AddonRepository;
import com.chargebee.models.Event;

@Service
public class AddonService {
    @Autowired
    private AddonRepository addonRepository;

    public boolean saveAddon(Event event) {
        var addon = event.content().addon();
        AddonEntity entity = new AddonEntity();
        entity.setId(addon.id());
        entity.setName(addon.name());
        entity.setDescription(addon.description());
        entity.setPrice(addon.price() != null ? java.math.BigDecimal.valueOf(addon.price()).movePointLeft(2) : null);
        entity.setCurrencyCode(addon.currencyCode());
        entity.setStatus(addon.status() != null ? addon.status().toString() : null);
        entity.setType(addon.type() != null ? addon.type().toString() : null);
        entity.setEnabledInPortal(addon.enabledInPortal());
        // entity.setEnabledForCheckout(addon.enabledForCheckout());
        addonRepository.save(entity);
        return true;
    }

    public boolean updateAddon(Event event) {
        var addon = event.content().addon();
        AddonEntity entity = addonRepository.findById(addon.id()).orElse(new AddonEntity());
        entity.setId(addon.id());
        entity.setName(addon.name());
        entity.setDescription(addon.description());
        entity.setPrice(addon.price() != null ? java.math.BigDecimal.valueOf(addon.price()).movePointLeft(2) : null);
        entity.setCurrencyCode(addon.currencyCode());
        entity.setStatus(addon.status() != null ? addon.status().toString() : null);
        entity.setType(addon.type() != null ? addon.type().toString() : null);
        entity.setEnabledInPortal(addon.enabledInPortal());
        // entity.setEnabledForCheckout(addon.enabledForCheckout());
        addonRepository.save(entity);
        return true;
    }

    public boolean deleteAddon(Event event) {
        var addon = event.content().addon();
        addonRepository.deleteById(addon.id());
        return true;
    }
}
