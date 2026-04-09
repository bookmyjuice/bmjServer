package com.bookmyjuice.models.mappers;

import com.bookmyjuice.models.entities.CustomerEntity;
import com.chargebee.models.Customer;

public class CustomerMapper {
    public static CustomerEntity toEntity(Customer chargebeeCustomer) {
         if (chargebeeCustomer.id() == null || chargebeeCustomer.id().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is missing");
        }
        CustomerEntity entity = new CustomerEntity();

        entity.setId(chargebeeCustomer.id());
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

        // Map Billing Address
        if (chargebeeCustomer.billingAddress() != null) {
            entity.setBillingAddress(mapBillingAddress(chargebeeCustomer.billingAddress()));
        }
        // Shipping address is not available on Customer in Chargebee Java SDK

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

    private static com.bookmyjuice.models.entities.BillingAddressEntity mapBillingAddress(Customer.BillingAddress billingAddress) {
        if (billingAddress == null) return null;
        com.bookmyjuice.models.entities.BillingAddressEntity entity = new com.bookmyjuice.models.entities.BillingAddressEntity();
        // entity.setId(billingAddress.id()); // Not available in Chargebee SDK
        entity.setFirstName(billingAddress.firstName());
        entity.setLastName(billingAddress.lastName());
        entity.setEmail(billingAddress.email());
        entity.setCompany(billingAddress.company());
        entity.setPhone(billingAddress.phone());
        entity.setLine1(billingAddress.line1());
        entity.setLine2(billingAddress.line2());
        entity.setCity(billingAddress.city());
        entity.setState(billingAddress.state());
        entity.setCountry(billingAddress.country());
        entity.setZip(billingAddress.zip());
        return entity;
    }

    // Shipping address mapping removed as it's not supported on Customer

    public static CustomerEntity fromId(String customerId) {
        CustomerEntity customer = new CustomerEntity();
        customer.setId(customerId);
        // Optionally, fetch from DB if needed, or just set ID for mapping
        return customer;
    }
}