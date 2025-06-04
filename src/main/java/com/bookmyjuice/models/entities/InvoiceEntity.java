package com.bookmyjuice.models.entities;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.lang.Nullable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice_entity")
public class InvoiceEntity {

    @Id
    private String id;

    @Nullable
    private String customerId;

    @Nullable
    private BigDecimal amount;

    @Nullable
    private String status;

    @Nullable
    private Date createdAt;

    @Nullable
    private String notes; // JSON representation of notes

    @Nullable
    private Date dueDate; // Additional field from Chargebee API

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }
}