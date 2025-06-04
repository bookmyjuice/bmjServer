package com.bookmyjuice.models.entities;

import com.chargebee.models.Subscription.SubscriptionItem.ItemType;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscription_item_entity")
public class SubscriptionItemEntity {
    @Id
    private String itemPriceId; // Unique identifier of the item price

    private ItemType itemType; // Type of item (PLAN, ADDON, CHARGE)

    @Nullable
    private Integer quantity; // Quantity of the item purchased (optional)

    @Nullable
    private String quantityInDecimal; // Decimal representation of the quantity (optional)

    @Nullable
    private Long unitPrice; // Price per unit in cents (optional)

    @Nullable
    private String unitPriceInDecimal; // Decimal representation of the unit price (optional)

    @Nullable
    private Long amount; // Total amount for the item in cents (optional)

    @Nullable
    private String amountInDecimal; // Decimal representation of the total amount (optional)

    @Nullable
    private Long currentTermStart; // Start of the item's current billing period (optional)

    @Nullable
    private Long currentTermEnd; // End of the item's current billing period (optional)

    @Nullable
    private Long nextBillingAt; // Next billing date for the item (optional)

    @Nullable
    private Integer billingPeriod; // Interval between consecutive billing cycles (optional)

    @Nullable
    private String billingPeriodUnit; // Unit of measurement for billing period (DAY, WEEK, MONTH, YEAR) (optional)

    @Nullable
    private Integer freeQuantity; // Free quantity included in the subscription (optional)

    @Nullable
    private String freeQuantityInDecimal; // Decimal representation of the free quantity (optional)

    @Nullable
    private Long trialEnd; // End of the trial period for the item (optional)

    @Nullable
    private Integer billingCycles; // Number of billing cycles for the item (optional)

    @Nullable
    private Integer servicePeriodDays; // Service period of the item in days (optional)

    @Nullable
    private String chargeOnEvent; // Event triggering the charge (optional)

    @Nullable
    private Boolean chargeOnce; // Indicates if the item is charged only once (optional)

    @Nullable
    private String chargeOnOption; // When the charge-item is to be charged (IMMEDIATELY, ON_EVENT) (optional)

    @Nullable
    private String prorationType; // How to manage charges or credits (FULL_TERM, PARTIAL_TERM, NONE) (optional)

    @Nullable
    private String usageAccumulationResetFrequency; // Frequency of usage counter reset (NEVER, SUBSCRIPTION_BILLING_FREQUENCY) (optional)

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private SubscriptionEntity subscription; // Reference to the parent subscription

    // Getters and Setters
    public String getItemPriceId() {
        return itemPriceId;
    }

    public void setItemPriceId(String itemPriceId) {
        this.itemPriceId = itemPriceId;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    @Nullable
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(@Nullable Integer quantity) {
        this.quantity = quantity;
    }

    @Nullable
    public String getQuantityInDecimal() {
        return quantityInDecimal;
    }

    public void setQuantityInDecimal(@Nullable String quantityInDecimal) {
        this.quantityInDecimal = quantityInDecimal;
    }

    @Nullable
    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(@Nullable Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    @Nullable
    public String getUnitPriceInDecimal() {
        return unitPriceInDecimal;
    }

    public void setUnitPriceInDecimal(@Nullable String unitPriceInDecimal) {
        this.unitPriceInDecimal = unitPriceInDecimal;
    }

    @Nullable
    public Long getAmount() {
        return amount;
    }

    public void setAmount(@Nullable Long amount) {
        this.amount = amount;
    }

    @Nullable
    public String getAmountInDecimal() {
        return amountInDecimal;
    }

    public void setAmountInDecimal(@Nullable String amountInDecimal) {
        this.amountInDecimal = amountInDecimal;
    }

    @Nullable
    public Long getCurrentTermStart() {
        return currentTermStart;
    }

    public void setCurrentTermStart(@Nullable Long currentTermStart) {
        this.currentTermStart = currentTermStart;
    }

    @Nullable
    public Long getCurrentTermEnd() {
        return currentTermEnd;
    }

    public void setCurrentTermEnd(@Nullable Long currentTermEnd) {
        this.currentTermEnd = currentTermEnd;
    }

    @Nullable
    public Long getNextBillingAt() {
        return nextBillingAt;
    }

    public void setNextBillingAt(@Nullable Long nextBillingAt) {
        this.nextBillingAt = nextBillingAt;
    }

    @Nullable
    public Integer getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(@Nullable Integer billingPeriod) {
        this.billingPeriod = billingPeriod;
    }

    @Nullable
    public String getBillingPeriodUnit() {
        return billingPeriodUnit;
    }

    public void setBillingPeriodUnit(@Nullable String billingPeriodUnit) {
        this.billingPeriodUnit = billingPeriodUnit;
    }

    @Nullable
    public Integer getFreeQuantity() {
        return freeQuantity;
    }

    public void setFreeQuantity(@Nullable Integer freeQuantity) {
        this.freeQuantity = freeQuantity;
    }

    @Nullable
    public String getFreeQuantityInDecimal() {
        return freeQuantityInDecimal;
    }

    public void setFreeQuantityInDecimal(@Nullable String freeQuantityInDecimal) {
        this.freeQuantityInDecimal = freeQuantityInDecimal;
    }

    @Nullable
    public Long getTrialEnd() {
        return trialEnd;
    }

    public void setTrialEnd(@Nullable Long trialEnd) {
        this.trialEnd = trialEnd;
    }

    @Nullable
    public Integer getBillingCycles() {
        return billingCycles;
    }

    public void setBillingCycles(@Nullable Integer billingCycles) {
        this.billingCycles = billingCycles;
    }

    @Nullable
    public Integer getServicePeriodDays() {
        return servicePeriodDays;
    }

    public void setServicePeriodDays(@Nullable Integer servicePeriodDays) {
        this.servicePeriodDays = servicePeriodDays;
    }

    @Nullable
    public String getChargeOnEvent() {
        return chargeOnEvent;
    }

    public void setChargeOnEvent(@Nullable String chargeOnEvent) {
        this.chargeOnEvent = chargeOnEvent;
    }

    @Nullable
    public Boolean getChargeOnce() {
        return chargeOnce;
    }

    public void setChargeOnce(@Nullable Boolean chargeOnce) {
        this.chargeOnce = chargeOnce;
    }

    @Nullable
    public String getChargeOnOption() {
        return chargeOnOption;
    }

    public void setChargeOnOption(@Nullable String chargeOnOption) {
        this.chargeOnOption = chargeOnOption;
    }

    @Nullable
    public String getProrationType() {
        return prorationType;
    }

    public void setProrationType(@Nullable String prorationType) {
        this.prorationType = prorationType;
    }

    @Nullable
    public String getUsageAccumulationResetFrequency() {
        return usageAccumulationResetFrequency;
    }

    public void setUsageAccumulationResetFrequency(@Nullable String usageAccumulationResetFrequency) {
        this.usageAccumulationResetFrequency = usageAccumulationResetFrequency;
    }

    public SubscriptionEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionEntity subscription) {
        this.subscription = subscription;
    }
}