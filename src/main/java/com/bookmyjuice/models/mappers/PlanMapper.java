package com.bookmyjuice.models.mappers;

import com.bookmyjuice.models.entities.PlanEntity;
import com.chargebee.models.Plan;

public class PlanMapper {
    public static PlanEntity toEntity(Plan plan) {
        PlanEntity entity = new PlanEntity();
        return toEntity(plan, entity);
    }

    public static PlanEntity toEntity(Plan plan, PlanEntity entity) {
        entity.setId(plan.id());
        entity.setName(plan.name());
        entity.setDescription(plan.description());
        entity.setPrice(plan.price() != null ? java.math.BigDecimal.valueOf(plan.price()).movePointLeft(2) : null);
        entity.setCurrencyCode(plan.currencyCode());
        entity.setPeriodUnit(plan.periodUnit() != null ? plan.periodUnit().toString() : null);
        entity.setPeriod(plan.period());
        entity.setStatus(plan.status() != null ? plan.status().toString() : null);
        entity.setEnabledInPortal(plan.enabledInPortal());
        entity.setInvoiceNotes(plan.invoiceNotes());
        entity.setMetaData(plan.metaData() != null ? plan.metaData().toString() : null);
        entity.setShowDescriptionInInvoices(plan.showDescriptionInInvoices());
        entity.setShowDescriptionInQuotes(plan.showDescriptionInQuotes());
        // Fields not present in SDK (externalName, enabledForCheckout, archived) are not set here
        return entity;
    }
}
