package com.bookmyjuice.models;

import jakarta.persistence.*;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    private String id; // e.g., "plan_delight_200_weekly"

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column
    private String size; // e.g., "200ml"

    @Column
    private String frequency; // "weekly" or "monthly"

    @Column
    private Long price; // in paise

    @Column
    private String currency = "INR";

    @Column
    private String billingPeriod; // e.g., "month" or "week"

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBillingPeriod() {
        return billingPeriod;
    }

    public void setBillingPeriod(String billingPeriod) {
        this.billingPeriod = billingPeriod;
    }
}
