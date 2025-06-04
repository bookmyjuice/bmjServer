package com.bookmyjuice.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.io.Serializable;

@Entity
@Table(name = "attached_item_entity")
public class AttachedItemEntity implements Serializable {
    @Id
    private String id; // Use Chargebee attached_item id or a composite key if needed

    private String parentItemId; // The item/addon/plan this is attached to
    private String attachedItemId; // The item/addon/plan being attached
    private String type; // e.g., "addon", "plan", "charge"
    private String status;
    private String billingCycles;
    private String quantity;
    private String quantityInDecimal;
    private String billingAlignmentMode;
    private String channel;

    @ManyToOne
    @JoinColumn(name = "addon_id")
    private AddonEntity addon;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getParentItemId() { return parentItemId; }
    public void setParentItemId(String parentItemId) { this.parentItemId = parentItemId; }
    public String getAttachedItemId() { return attachedItemId; }
    public void setAttachedItemId(String attachedItemId) { this.attachedItemId = attachedItemId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBillingCycles() { return billingCycles; }
    public void setBillingCycles(String billingCycles) { this.billingCycles = billingCycles; }
    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public String getQuantityInDecimal() { return quantityInDecimal; }
    public void setQuantityInDecimal(String quantityInDecimal) { this.quantityInDecimal = quantityInDecimal; }
    public String getBillingAlignmentMode() { return billingAlignmentMode; }
    public void setBillingAlignmentMode(String billingAlignmentMode) { this.billingAlignmentMode = billingAlignmentMode; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public AddonEntity getAddon() { return addon; }
    public void setAddon(AddonEntity addon) { this.addon = addon; }
}
