package com.bookmyjuice.models.mappers;

import com.bookmyjuice.models.entities.ShippingAddressEntity;
import com.bookmyjuice.models.entities.SubscriptionEntity;
import com.chargebee.models.Subscription;

public class SubscriptionMapper {
    public static SubscriptionEntity toEntity(com.chargebee.models.Subscription chargebeeSubscription) {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setId(chargebeeSubscription.id()); // Convert to String if necessary
        entity.setBillingPeriod((int) chargebeeSubscription.billingPeriod());
        entity.setBillingPeriodUnit(chargebeeSubscription.billingPeriodUnit().toString());
        entity.setAutoCollection(chargebeeSubscription.autoCollection() != null
                ? chargebeeSubscription.autoCollection().toString()
                : "UNKNOWN");
        entity.setStatus(chargebeeSubscription.status().toString());
        entity.setCreatedAt((int) chargebeeSubscription.createdAt().getTime());
        entity.setStartedAt((int) chargebeeSubscription.startedAt().getTime());
        // entity.setCancelledAt((int) chargebeeSubscription.cancelledAt().getTime());
        entity.setUpdatedAt((int) chargebeeSubscription.updatedAt().getTime());
        entity.setHasScheduledChanges((boolean) chargebeeSubscription.hasScheduledChanges());
        entity.setChannel(chargebeeSubscription.channel().toString());
        entity.setResourceVersion((long) chargebeeSubscription.resourceVersion());
        entity.setDeleted((boolean) chargebeeSubscription.deleted());
        entity.setCurrencyCode(chargebeeSubscription.currencyCode());
        entity.setDueInvoicesCount((int) chargebeeSubscription.dueInvoicesCount());
        entity.setMrr(chargebeeSubscription.mrr() != null ? (long) chargebeeSubscription.mrr() : 0L);
        entity.setHasScheduledAdvanceInvoices((boolean) chargebeeSubscription.hasScheduledAdvanceInvoices());
        // Map Shipping Address if present
        if (chargebeeSubscription.shippingAddress() != null) {
            entity.setShippingAddress(mapShippingAddress(chargebeeSubscription.shippingAddress()));
        }
        // entity.setCustomerId(chargebeeSubscription.customerId());
        return entity;
    }

    private static ShippingAddressEntity mapShippingAddress(Subscription.ShippingAddress shippingAddress) {
        ShippingAddressEntity entity = new ShippingAddressEntity();
        entity.setLine1(shippingAddress.line1());
        entity.setLine2(shippingAddress.line2());
        entity.setCity(shippingAddress.city());
        entity.setState(shippingAddress.state());
        entity.setCountry(shippingAddress.country());
        entity.setZip(shippingAddress.zip());
        entity.setEmail(shippingAddress.email());
        entity.setPhone(shippingAddress.phone());
        return entity;
    }
}
