package com.bookmyjuice.models;

import com.chargebee.models.Customer;

public class CustomerMapper {
    public static CustomerEntity toEntity(Customer chargebeeCustomer) {
        CustomerEntity entity = new CustomerEntity();

        entity.setCustomerId(chargebeeCustomer.id());
        entity.setFirstName(chargebeeCustomer.firstName());
        entity.setLastName(chargebeeCustomer.lastName());
        entity.setEmail(chargebeeCustomer.email());
        entity.setPhone(chargebeeCustomer.phone());
        entity.setCompany(chargebeeCustomer.company());
        entity.setAutoCollection(chargebeeCustomer.autoCollection().toString());
        entity.setNetTermDays(chargebeeCustomer.netTermDays());
        entity.setAllowDirectDebit(chargebeeCustomer.allowDirectDebit());
        entity.setCreatedAt(chargebeeCustomer.createdAt().getTime());
        entity.setTaxability(chargebeeCustomer.taxability().toString());
        entity.setUpdatedAt(chargebeeCustomer.updatedAt().getTime());
        entity.setPiiCleared(chargebeeCustomer.piiCleared().toString());
        entity.setChannel(chargebeeCustomer.channel().toString());
        entity.setResourceVersion(chargebeeCustomer.resourceVersion());
        entity.setDeleted(chargebeeCustomer.deleted());
        
        entity.setPromotionalCredits(chargebeeCustomer.promotionalCredits());
        entity.setRefundableCredits(chargebeeCustomer.refundableCredits());
        entity.setExcessPayments(chargebeeCustomer.excessPayments());
        entity.setUnbilledCharges(chargebeeCustomer.unbilledCharges());
        entity.setPreferredCurrencyCode(chargebeeCustomer.preferredCurrencyCode());
        // entity.setMrr(chargebeeCustomer.mrr());
        entity.setPrimaryPaymentSourceId(chargebeeCustomer.primaryPaymentSourceId());

        // Convert PaymentMethod
        // if (chargebeeCustomer.paymentMethod() != null) {
        //     entity.setPaymentMethod(PaymentMethodMapper.toEntity(chargebeeCustomer.getPaymentMethod()));
        // }

        // Convert Subscriptions
        // if (chargebeeCustomer. != null) {
        //     List<SubscriptionEntity> subscriptions = chargebeeCustomer.
        //         .stream()
        //         .map(SubscriptionMapper::toEntity)
        //         .toList();
        //     entity.setSubscriptions(subscriptions);
        // }

        return entity;
    }
}
