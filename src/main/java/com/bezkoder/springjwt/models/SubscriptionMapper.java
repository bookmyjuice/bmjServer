package com.bezkoder.springjwt.models;

import java.util.Map;

public class SubscriptionMapper {
    public static SubscriptionEntity toEntity(Map<String, Object> chargebeeSubscription) {
        SubscriptionEntity entity = new SubscriptionEntity();

        entity.setId(chargebeeSubscription.get("id").toString());
        entity.setBillingPeriod((int) chargebeeSubscription.get("billing_period"));
        entity.setBillingPeriodUnit(chargebeeSubscription.get("billing_period_unit").toString());
        entity.setAutoCollection(chargebeeSubscription.get("auto_collection").toString());
        entity.setStatus(chargebeeSubscription.get("status").toString());
        entity.setCreatedAt((int) chargebeeSubscription.get("created_at"));

        entity.setStartedAt((int) chargebeeSubscription.get("started_at"));
        entity.setCancelledAt((int) chargebeeSubscription.get("cancelled_at"));
        entity.setUpdatedAt((int) chargebeeSubscription.get("updated_at"));
        entity.setHasScheduledChanges((boolean) chargebeeSubscription.get("has_scheduled_changes"));
        entity.setChannel(chargebeeSubscription.get("channel").toString());
        entity.setResourceVersion((long) chargebeeSubscription.get("resource_version"));
        entity.setDeleted((boolean) chargebeeSubscription.get("deleted"));
        entity.setCurrencyCode(chargebeeSubscription.get("currency_code").toString());
        entity.setDueInvoicesCount((int) chargebeeSubscription.get("due_invoices_count"));
        entity.setMrr((int) chargebeeSubscription.get("mrr"));
        entity.setHasScheduledAdvanceInvoices((boolean) chargebeeSubscription.get("has_scheduled_advance_invoices"));
        entity.setCustomerId(chargebeeSubscription.get("customer_id").toString());
        return entity;
    }
}
