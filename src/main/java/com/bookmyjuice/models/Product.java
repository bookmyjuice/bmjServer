package com.bookmyjuice.models;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private String id; // e.g., "juice_abc"

    @Column(nullable = false)
    private String name;

    @Column
    private String category; // e.g., "Delight", "Signature", "Premium"

    @Column
    private String imageUrl;

    @Column
    private String itemType; // "charge" (One-Time) or "plan" (Subscription)

    // Metadata stored as JSON (e.g., tags, description)
    @Column(columnDefinition = "TEXT")
    private String metadata;

    // One-Time Price
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private OneTimePrice oneTimePrice;

    // Subscription Plans
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.Set<SubscriptionPlan> subscriptionPlans;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public OneTimePrice getOneTimePrice() {
        return oneTimePrice;
    }

    public void setOneTimePrice(OneTimePrice oneTimePrice) {
        this.oneTimePrice = oneTimePrice;
    }

    public java.util.Set<SubscriptionPlan> getSubscriptionPlans() {
        return subscriptionPlans;
    }

    public void setSubscriptionPlans(java.util.Set<SubscriptionPlan> subscriptionPlans) {
        this.subscriptionPlans = subscriptionPlans;
    }
}
