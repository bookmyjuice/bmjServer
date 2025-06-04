package com.bookmyjuice.models.entities;

import java.math.BigDecimal;
import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "addon_entity")
public class AddonEntity {
    @Id
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String currencyCode;
    private String status;
    @Nullable
    private String type; // optional
    @Nullable
    private Boolean enabledInPortal; // optional
    @Nullable
    private Boolean enabledForCheckout; // optional

    @OneToMany(mappedBy = "addon", fetch = FetchType.LAZY)
    private List<AttachedItemEntity> attachedItems; // All attached items for this addon
    // Add more fields as needed from Chargebee Addon API

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Boolean getEnabledInPortal() { return enabledInPortal; }
    public void setEnabledInPortal(Boolean enabledInPortal) { this.enabledInPortal = enabledInPortal; }
    public Boolean getEnabledForCheckout() { return enabledForCheckout; }
    public void setEnabledForCheckout(Boolean enabledForCheckout) { this.enabledForCheckout = enabledForCheckout; }
    public List<AttachedItemEntity> getAttachedItems() { return attachedItems; }
    public void setAttachedItems(List<AttachedItemEntity> attachedItems) { this.attachedItems = attachedItems; }
}
