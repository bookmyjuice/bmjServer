package com.bookmyjuice.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "subscription_entity")
public class SubscriptionEntity {
    @Id
    private String id;
    private int billingPeriod;
    private String billingPeriodUnit;
    private String autoCollection;
    private String status;
    private int createdAt;
    private int startedAt;
    private int cancelledAt;
    private int updatedAt;
    private boolean hasScheduledChanges;
    private String channel;
    private long resourceVersion;
    private boolean deleted;
    private String currencyCode;
    private int dueInvoicesCount;
    private Long mrr;
    private boolean hasScheduledAdvanceInvoices;
    private String customerId;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(int billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    public String getBillingPeriodUnit() {
        return billingPeriodUnit;
    }

    public void setBillingPeriodUnit(String billingPeriodUnit) {
        this.billingPeriodUnit = billingPeriodUnit;
    }

    public String getAutoCollection() {
        return autoCollection;
    }

    public void setAutoCollection(String autoCollection) {
        this.autoCollection = autoCollection;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public int getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(int createdAt) {
        this.createdAt = createdAt;
    }
    
    public int getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(int startedAt) {
        this.startedAt = startedAt;
    }
    
    public int getCancelledAt() {
        return cancelledAt;
    }
    
    public void setCancelledAt(int cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    public int getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(int updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public boolean isHasScheduledChanges() {
        return hasScheduledChanges;
    }
    
    public void setHasScheduledChanges(boolean hasScheduledChanges) {
        this.hasScheduledChanges = hasScheduledChanges;
    }
    
    public String getChannel() {
        return channel;
    }
    
    public void setChannel(String channel) {
        this.channel = channel;
    }
    
    public Long getResourceVersion() {
        return resourceVersion;
    }
    
    public void setResourceVersion(Long resourceVersion) {
        this.resourceVersion = resourceVersion;
    }
    
    public boolean isDeleted() {
        return deleted;
    }
    
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    
    public int getDueInvoicesCount() {
        return dueInvoicesCount;
    }
    
    public void setDueInvoicesCount(int dueInvoicesCount) {
        this.dueInvoicesCount = dueInvoicesCount;
    }
    
    public Long getMrr() {
        return mrr;
    }
    
    public void setMrr(Long mrr) {
        this.mrr = mrr;
    }
    
    public boolean isHasScheduledAdvanceInvoices() {
        return hasScheduledAdvanceInvoices;
    }
    
    public void setHasScheduledAdvanceInvoices(boolean hasScheduledAdvanceInvoices) {
        this.hasScheduledAdvanceInvoices = hasScheduledAdvanceInvoices;
    }


    // public CustomerEntity getCustomer() {
    //     return customer;
    // }

    // public void setCustomer(CustomerEntity customer) {
    //     this.customer = customer;
    // }

    // public List<SubscriptionItemEntity> getSubscriptionItems() {
    //     return subscriptionItems;
    // }

    // public void setSubscriptionItems(List<SubscriptionItemEntity> subscriptionItems) {
    //     this.subscriptionItems = subscriptionItems;
    // }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}