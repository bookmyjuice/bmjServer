package com.bookmyjuice.models.mappers;

import com.bookmyjuice.models.entities.SubscriptionItemEntity;
import com.chargebee.models.Subscription.SubscriptionItem;

public class SubscriptionItemMapper {

    // Convert Subscription.SubscriptionItem (Chargebee) to SubscriptionItemEntity
    public static SubscriptionItemEntity toEntity(SubscriptionItem dto) {
        if (dto == null) {
            return null;
        }

        SubscriptionItemEntity entity = new SubscriptionItemEntity();
    entity.setItemPriceId(dto.itemPriceId());
    entity.setItemType(dto.itemType());
    entity.setQuantity(dto.quantity());
    entity.setQuantityInDecimal(dto.quantityInDecimal());
    entity.setUnitPrice(dto.unitPrice());
    entity.setUnitPriceInDecimal(dto.unitPriceInDecimal());
    entity.setAmount(dto.amount());
    entity.setAmountInDecimal(dto.amountInDecimal());
    entity.setCurrentTermStart(dto.currentTermStart() != null ? dto.currentTermStart().getTime() : null);
    entity.setCurrentTermEnd(dto.currentTermEnd() != null ? dto.currentTermEnd().getTime() : null);
    entity.setNextBillingAt(dto.nextBillingAt() != null ? dto.nextBillingAt().getTime() : null);
    entity.setTrialEnd(dto.trialEnd() != null ? dto.trialEnd().getTime() : null);
    entity.setBillingPeriod(dto.billingPeriod());
    entity.setBillingPeriodUnit(dto.billingPeriodUnit() != null ? dto.billingPeriodUnit().name() : null);
    entity.setFreeQuantity(dto.freeQuantity());
    entity.setFreeQuantityInDecimal(dto.freeQuantityInDecimal());
    entity.setBillingCycles(dto.billingCycles());
    entity.setServicePeriodDays(dto.servicePeriodDays());
    entity.setChargeOnEvent(dto.chargeOnEvent() != null ? dto.chargeOnEvent().name() : null);
    entity.setChargeOnce(dto.chargeOnce());
    entity.setChargeOnOption(dto.chargeOnOption() != null ? dto.chargeOnOption().name() : null);
    entity.setProrationType(dto.prorationType() != null ? dto.prorationType().name() : null);
    entity.setUsageAccumulationResetFrequency(dto.usageAccumulationResetFrequency() != null ? dto.usageAccumulationResetFrequency().name() : null);
    // TODO: Map discounts, contract terms, coupons, metadata, etc. if entity supports
    // Note: SubscriptionEntity should be set separately in the service layer
    return entity;
    }
}