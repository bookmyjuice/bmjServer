package com.bookmyjuice.models.mappers;
import java.util.List;

import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.models.entities.ShippingAddressEntity;
import com.bookmyjuice.models.entities.SubscriptionEntity;
import com.chargebee.models.Subscription;

public class SubscriptionMapper {
    public static SubscriptionEntity toEntity(com.chargebee.models.Subscription chargebeeSubscription) {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setId(chargebeeSubscription.id());
        entity.setBillingPeriod((int) chargebeeSubscription.billingPeriod());
        entity.setBillingPeriodUnit(chargebeeSubscription.billingPeriodUnit() != null ? chargebeeSubscription.billingPeriodUnit().toString() : null);
        entity.setAutoCollection(chargebeeSubscription.autoCollection() != null ? chargebeeSubscription.autoCollection().toString() : null);
            entity.setStatus(chargebeeSubscription.status() != null ? chargebeeSubscription.status().toString() : null);
        entity.setCreatedAt(chargebeeSubscription.createdAt() != null ? (int) chargebeeSubscription.createdAt().getTime() : 0);
        entity.setStartedAt(chargebeeSubscription.startedAt() != null ? (int) chargebeeSubscription.startedAt().getTime() : 0);
        entity.setCancelledAt(chargebeeSubscription.cancelledAt() != null ? (int) chargebeeSubscription.cancelledAt().getTime() : 0);
        entity.setUpdatedAt(chargebeeSubscription.updatedAt() != null ? (int) chargebeeSubscription.updatedAt().getTime() : 0);
        entity.setHasScheduledChanges(chargebeeSubscription.hasScheduledChanges());
        entity.setChannel(chargebeeSubscription.channel() != null ? chargebeeSubscription.channel().toString() : null);
        entity.setResourceVersion(chargebeeSubscription.resourceVersion());
        entity.setDeleted(chargebeeSubscription.deleted());
        entity.setCurrencyCode(chargebeeSubscription.currencyCode());
    entity.setDueInvoicesCount(chargebeeSubscription.dueInvoicesCount() != null ? chargebeeSubscription.dueInvoicesCount() : 0);
    entity.setMrr(chargebeeSubscription.mrr() != null ? Long.valueOf(chargebeeSubscription.mrr()) : 0L);
        entity.setHasScheduledAdvanceInvoices(chargebeeSubscription.hasScheduledAdvanceInvoices());
        // entity.setCustomerId(chargebeeSubscription.customerId());
        // Ensure customer is always set
        String customerId = chargebeeSubscription.customerId();
        if (customerId == null || customerId.isEmpty()) {
            throw new IllegalArgumentException("Subscription must have a valid customerId");
        }
        CustomerEntity customer = CustomerMapper.fromId(customerId); // You may need to implement this method to fetch or create CustomerEntity
        entity.setCustomer(customer);
        // Map Shipping Address if present
        if (chargebeeSubscription.shippingAddress() != null) {
            entity.setShippingAddress(mapShippingAddress(chargebeeSubscription.shippingAddress()));
        }
        // Map discounts, contract terms, coupons, etc. if present
        // entity.setDiscounts(...); // TODO: Implement if entity supports
        // entity.setContractTerm(...); // TODO: Implement if entity supports
        // Map subscription items
        if (chargebeeSubscription.subscriptionItems() != null) {
            List<com.bookmyjuice.models.entities.SubscriptionItemEntity> itemEntities = chargebeeSubscription.subscriptionItems().stream()
                .map(com.bookmyjuice.models.mappers.SubscriptionItemMapper::toEntity)
                .toList();
            entity.setSubscriptionItems(itemEntities);
        }
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
