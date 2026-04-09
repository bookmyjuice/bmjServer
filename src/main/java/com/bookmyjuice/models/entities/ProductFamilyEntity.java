package com.bookmyjuice.models.entities;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Product Family Entity - Represents product categories (Delight, Signature, Premium)
 * 
 * Relationship: product_families (1) ───────────────── (N) items
 *               [items.product_family_id = product_families.id]
 */
@Entity
@Table(name = "product_families")
public class ProductFamilyEntity {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String status;
    
    @OneToMany(mappedBy = "productFamily", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ItemEntity> items = new ArrayList<>();

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ItemEntity> getItems() {
        return items;
    }

    public void setItems(List<ItemEntity> items) {
        this.items = items;
    }

    // Helper methods
    public void addItem(ItemEntity item) {
        items.add(item);
        item.setProductFamily(this);
    }

    public void removeItem(ItemEntity item) {
        items.remove(item);
        item.setProductFamily(null);
    }
}
