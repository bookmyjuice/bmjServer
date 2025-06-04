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
        entity.setItemPriceId(dto.itemPriceId()); // Map itemPriceId
        entity.setItemType(dto.itemType()); // Map itemType (PLAN, ADDON, CHARGE)
        entity.setQuantity(dto.quantity()); // Map quantity
        entity.setQuantityInDecimal(dto.quantityInDecimal()); // Map quantityInDecimal
        entity.setUnitPrice(dto.unitPrice()); // Map unitPrice
        entity.setUnitPriceInDecimal(dto.unitPriceInDecimal()); // Map unitPriceInDecimal
        entity.setAmount(dto.amount()); // Map amount
        entity.setAmountInDecimal(dto.amountInDecimal()); // Map amountInDecimal

        // Handle DateTime to Long conversions
        entity.setCurrentTermStart(dto.currentTermStart() != null ? dto.currentTermStart().getTime() : null); // Map currentTermStart
        entity.setCurrentTermEnd(dto.currentTermEnd() != null ? dto.currentTermEnd().getTime() : null); // Map currentTermEnd
        entity.setNextBillingAt(dto.nextBillingAt() != null ? dto.nextBillingAt().getTime() : null); // Map nextBillingAt
        entity.setTrialEnd(dto.trialEnd() != null ? dto.trialEnd().getTime() : null); // Map trialEnd

        entity.setBillingPeriod(dto.billingPeriod()); // Map billingPeriod
        entity.setBillingPeriodUnit(dto.billingPeriodUnit() != null ? dto.billingPeriodUnit().name() : null); // Map billingPeriodUnit (DAY, WEEK, MONTH, YEAR)
        entity.setFreeQuantity(dto.freeQuantity()); // Map freeQuantity
        entity.setFreeQuantityInDecimal(dto.freeQuantityInDecimal()); // Map freeQuantityInDecimal
        entity.setBillingCycles(dto.billingCycles()); // Map billingCycles
        entity.setServicePeriodDays(dto.servicePeriodDays()); // Map servicePeriodDays
        entity.setChargeOnEvent(dto.chargeOnEvent() != null ? dto.chargeOnEvent().name() : null); // Map chargeOnEvent
        entity.setChargeOnce(dto.chargeOnce()); // Map chargeOnce
        entity.setChargeOnOption(dto.chargeOnOption() != null ? dto.chargeOnOption().name() : null); // Map chargeOnOption (IMMEDIATELY, ON_EVENT)
        entity.setProrationType(dto.prorationType() != null ? dto.prorationType().name() : null); // Map prorationType (FULL_TERM, PARTIAL_TERM, NONE)
        entity.setUsageAccumulationResetFrequency(dto.usageAccumulationResetFrequency() != null ? dto.usageAccumulationResetFrequency().name() : null); // Map usageAccumulationResetFrequency

        // Note: SubscriptionEntity should be set separately in the service layer
        return entity;
    }
}