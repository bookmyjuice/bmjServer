package com.bookmyjuice.models.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
    private int trialStart;
    private int trialEnd;
    private int nextBillingAt;
    private int pausedAt;
    private int resumeAt;
    private String currentTermStart;
    private String currentTermEnd;
    private String planId;
    private int planQuantity;
    private int planUnitPrice;
    private int planAmount;
    private String planFreeQuantity;
    private String planBillingCycles;
    private String addons;
    private String coupons;
    private String invoiceNotes;
    private boolean renewed;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerEntity customer;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ItemEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<SubscriptionItemEntity> subscriptionItems = new ArrayList<>();

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "shipping_address_id")
    private ShippingAddressEntity shippingAddress;

    // Getters and Setters for subscriptionItems
    public List<SubscriptionItemEntity> getSubscriptionItems() {
        return subscriptionItems;
    }

    public void setSubscriptionItems(List<SubscriptionItemEntity> subscriptionItems) {
        this.subscriptionItems.clear(); // Clear the existing collection
        if (subscriptionItems != null) {
            this.subscriptionItems.addAll(subscriptionItems); // Add the new items to the existing collection
        }
    }

    

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

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public List<ItemEntity> getItems() {
        return items;
    }

    public void setItems(List<ItemEntity> items) {
        this.items.clear(); // Clear the existing collection
        if (items != null) {
            this.items.addAll(items); // Add the new items to the existing collection
        }
    }

    public int getTrialStart() {
        return trialStart;
    }

    public void setTrialStart(int trialStart) {
        this.trialStart = trialStart;
    }

    public int getTrialEnd() {
        return trialEnd;
    }

    public void setTrialEnd(int trialEnd) {
        this.trialEnd = trialEnd;
    }

    public int getNextBillingAt() {
        return nextBillingAt;
    }

    public void setNextBillingAt(int nextBillingAt) {
        this.nextBillingAt = nextBillingAt;
    }

    public int getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(int pausedAt) {
        this.pausedAt = pausedAt;
    }

    public int getResumeAt() {
        return resumeAt;
    }

    public void setResumeAt(int resumeAt) {
        this.resumeAt = resumeAt;
    }

    public String getCurrentTermStart() {
        return currentTermStart;
    }

    public void setCurrentTermStart(String currentTermStart) {
        this.currentTermStart = currentTermStart;
    }

    public String getCurrentTermEnd() {
        return currentTermEnd;
    }

    public void setCurrentTermEnd(String currentTermEnd) {
        this.currentTermEnd = currentTermEnd;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public int getPlanQuantity() {
        return planQuantity;
    }

    public void setPlanQuantity(int planQuantity) {
        this.planQuantity = planQuantity;
    }

    public int getPlanUnitPrice() {
        return planUnitPrice;
    }

    public void setPlanUnitPrice(int planUnitPrice) {
        this.planUnitPrice = planUnitPrice;
    }

    public int getPlanAmount() {
        return planAmount;
    }

    public void setPlanAmount(int planAmount) {
        this.planAmount = planAmount;
    }

    public String getPlanFreeQuantity() {
        return planFreeQuantity;
    }

    public void setPlanFreeQuantity(String planFreeQuantity) {
        this.planFreeQuantity = planFreeQuantity;
    }

    public String getPlanBillingCycles() {
        return planBillingCycles;
    }

    public void setPlanBillingCycles(String planBillingCycles) {
        this.planBillingCycles = planBillingCycles;
    }

    public String getAddons() {
        return addons;
    }

    public void setAddons(String addons) {
        this.addons = addons;
    }

    public String getCoupons() {
        return coupons;
    }

    public void setCoupons(String coupons) {
        this.coupons = coupons;
    }

    public String getInvoiceNotes() {
        return invoiceNotes;
    }

    public void setInvoiceNotes(String invoiceNotes) {
        this.invoiceNotes = invoiceNotes;
    }

    public boolean isRenewed() {
        return renewed;
    }

    public void setRenewed(boolean renewed) {
        this.renewed = renewed;
    }
    public ShippingAddressEntity getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(ShippingAddressEntity shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}