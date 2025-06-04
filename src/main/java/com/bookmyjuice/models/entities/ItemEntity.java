package com.bookmyjuice.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_entity")
public class ItemEntity {

    @Id
    private String id;
    private String name;
    private String description;
    private String type;
    private String status;

    private String metaData;
    private String externalName;
    private boolean enabledInPortal;
    private boolean enabledForCheckout;
    private String itemFamilyId;
    private String unit;
    private boolean archived;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private SubscriptionEntity subscription;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExternalName() {
        return externalName;
    }

    public void setExternalName(String externalName) {
        this.externalName = externalName;
    }

    public boolean isEnabledInPortal() {
        return enabledInPortal;
    }

    public void setEnabledInPortal(boolean enabledInPortal) {
        this.enabledInPortal = enabledInPortal;
    }

    public boolean isEnabledForCheckout() {
        return enabledForCheckout;
    }

    public void setEnabledForCheckout(boolean enabledForCheckout) {
        this.enabledForCheckout = enabledForCheckout;
    }

    public String getItemFamilyId() {
        return itemFamilyId;
    }

    public void setItemFamilyId(String itemFamilyId) {
        this.itemFamilyId = itemFamilyId;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public SubscriptionEntity getSubscription() {
        return subscription;
    }

    public void setSubscription(SubscriptionEntity subscription) {
        this.subscription = subscription;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
