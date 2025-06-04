package com.bookmyjuice.models.entities;

import java.math.BigDecimal;
import java.util.Date;

import com.chargebee.models.Transaction.Status;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_entity")
public class PaymentEntity {
    @Id
    private String id;
    private String customerId;
    private BigDecimal amount;
    private Status status;
    private Date createdAt;
    private String invoiceId; // Add invoiceId field
    private String transactionId;
    private String currencyCode;
    private Date date;
    private String invoiceStatus; // Add invoiceStatus field

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Status getStatus() { return status; } // Getter for status
    public void setStatus(Status status) { this.status = status; } // Setter for status
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getInvoiceId() { return invoiceId; } // Getter for invoiceId
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; } // Setter for invoiceId
    public String getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    public String getCurrencyCode() {
        return currencyCode;
    }
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    public String getInvoiceStatus() {
        return invoiceStatus;
    }
    public void setInvoiceStatus(String invoiceStatus) {
        this.invoiceStatus = invoiceStatus;
    }
}
