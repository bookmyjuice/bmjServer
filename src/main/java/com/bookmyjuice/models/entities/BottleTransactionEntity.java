package com.bookmyjuice.models.entities;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * Records every bottle-related event in the system.
 * 
 * Actions:
 * - ISSUED: Bottles handed to customer as part of order fulfillment
 * - RETURNED: Customer returns bottles after consumption
 * - BROKEN: Customer reports a broken/lost bottle
 * 
 * This table drives the BottleLedger computed view.
 * Queries should use customerId + action aggregates to derive outstanding balances.
 */
@Entity
@Table(name = "bottle_transactions", indexes = {
    @Index(name = "idx_bottle_customer", columnList = "customerId"),
    @Index(name = "idx_bottle_order", columnList = "orderId"),
    @Index(name = "idx_bottle_action", columnList = "action")
})
public class BottleTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Links to the order or subscription delivery that triggered this bottle event.
     */
    @Column(nullable = false, length = 64)
    private String orderId;

    /**
     * Chargebee customer ID (or local user ID as fallback).
     * Used to aggregate bottle balances per customer.
     */
    @Column(nullable = false, length = 64)
    private String customerId;

    /**
     * Bottle type/format (e.g. "glass_250ml", "glass_500ml", "plastic_1l").
     * Reflects the packaging format used at dispatch time.
     */
    @Column(nullable = false, length = 32)
    private String bottleType;

    /**
     * Number of bottles involved in this transaction.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Action type: ISSUED | RETURNED | BROKEN
     */
    @Column(nullable = false, length = 16)
    private String action;

    /**
     * Optional reference (e.g. subscription delivery ID, return pickup ID).
     */
    @Column(length = 64)
    private String referenceId;

    /**
     * Optional notes (e.g. "2 bottles cracked during delivery").
     */
    @Column(length = 255)
    private String notes;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updatedAt;

    // ==================== Constructors ====================

    public BottleTransactionEntity() {
        Date now = new Date();
        this.createdAt = now;
        this.updatedAt = now;
    }

    // ==================== Getters & Setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getBottleType() { return bottleType; }
    public void setBottleType(String bottleType) { this.bottleType = bottleType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
