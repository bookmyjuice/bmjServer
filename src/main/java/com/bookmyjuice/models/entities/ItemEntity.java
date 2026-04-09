package com.bookmyjuice.models.entities;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

    @Lob
    @Column(columnDefinition = "TEXT")
    private String metaData;
    private String externalName;
    private boolean enabledInPortal;
    private boolean enabledForCheckout;

    /**
     * Product Family ID - Links to product_families table
     * Relationship: items.product_family_id = product_families.id (N:1)
     */
    @Column(name = "product_family_id")
    private String productFamilyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_family_id", insertable = false, updatable = false)
    private ProductFamilyEntity productFamily;

    private String unit;
    private boolean archived;
    private boolean giftable;
    private boolean shippable;
    private boolean deleted;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String jsonObject;

    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private SubscriptionEntity subscription;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ItemPriceEntity> itemPrices = new ArrayList<>();

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

    public String getProductFamilyId() {
        return productFamilyId;
    }

    public void setProductFamilyId(String productFamilyId) {
        this.productFamilyId = productFamilyId;
    }

    public ProductFamilyEntity getProductFamily() {
        return productFamily;
    }

    public void setProductFamily(ProductFamilyEntity productFamily) {
        this.productFamily = productFamily;
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

    public boolean isGiftable() {
        return giftable;
    }

    public void setGiftable(boolean giftable) {
        this.giftable = giftable;
    }

    public boolean isShippable() {
        return shippable;
    }

    public void setShippable(boolean shippable) {
        this.shippable = shippable;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(String jsonObject) {
        this.jsonObject = jsonObject;
    }

    // JSON utility methods for metadata
    @JsonIgnore
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get metadata as JsonNode for easy access to JSON fields
     */
    @JsonIgnore
    public JsonNode getMetaDataAsJson() {
        if (metaData == null || metaData.trim().isEmpty()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(metaData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Invalid JSON in metadata: " + e.getMessage());
        }
    }

    /**
     * Set metadata from Object (will be converted to JSON string)
     */
    public void setMetaDataFromObject(Object metadata) {
        if (metadata == null) {
            this.metaData = null;
            return;
        }
        try {
            this.metaData = objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert metadata to JSON: " + e.getMessage());
        }
    }

    /**
     * Get specific field from metadata JSON
     */
    @JsonIgnore
    public String getMetaDataField(String fieldName) {
        JsonNode jsonNode = getMetaDataAsJson();
        JsonNode field = jsonNode.get(fieldName);
        return field != null ? field.asText() : null;
    }

    /**
     * Get specific field from metadata JSON as Integer
     */
    @JsonIgnore
    public Integer getMetaDataFieldAsInt(String fieldName) {
        JsonNode jsonNode = getMetaDataAsJson();
        JsonNode field = jsonNode.get(fieldName);
        return field != null && !field.isNull() ? field.asInt() : null;
    }

    /**
     * Get specific field from metadata JSON as String array
     */
    @JsonIgnore
    public String[] getMetaDataFieldAsArray(String fieldName) {
        JsonNode jsonNode = getMetaDataAsJson();
        JsonNode field = jsonNode.get(fieldName);
        if (field != null && field.isArray()) {
            String[] result = new String[field.size()];
            for (int i = 0; i < field.size(); i++) {
                result[i] = field.get(i).asText();
            }
            return result;
        }
        return new String[0];
    }

    public List<ItemPriceEntity> getItemPrices() {
        return itemPrices;
    }

    public void setItemPrices(List<ItemPriceEntity> itemPrices) {
        this.itemPrices = itemPrices;
    }

    /**
     * Helper method to add an item price to this item
     */
    public void addItemPrice(ItemPriceEntity itemPrice) {
        itemPrices.add(itemPrice);
        itemPrice.setItem(this);
    }

    /**
     * Helper method to remove an item price from this item
     */
    public void removeItemPrice(ItemPriceEntity itemPrice) {
        itemPrices.remove(itemPrice);
        itemPrice.setItem(null);
    }
}
