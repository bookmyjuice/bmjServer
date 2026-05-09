package com.bookmyjuice.models;

import jakarta.persistence.*;

@Entity
@Table(name = "one_time_prices")
public class OneTimePrice {

    @Id
    private String id; // e.g., "charge_abc_200"

    @OneToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column
    private String size; // e.g., "200ml"

    @Column
    private Long price; // in paise

    @Column
    private String currency = "INR";

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
}
