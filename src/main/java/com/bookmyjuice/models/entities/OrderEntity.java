package com.bookmyjuice.models.entities;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_entity")
public class OrderEntity {
    @Id
    private String id;
    private String customerId;
    private String status;
    private Date createdAt;
    private String shippingAddress;
    private String billingAddress;
    private String documentNumber;
    private String invoiceId;
    private String paymentStatus;
    private String orderType;
    private String priceType;
    private Date orderDate;
    private Date shippingDate;
    private String createdBy;
    private Double tax;
    private Double amountPaid;
    private Double amountAdjusted;
    private Double refundableCreditsIssued;
    private Double refundableCredits;
    private Double roundingAdjustement;
    private Date paidOn;
    private Double exchangeRate;
    private Date updatedAt;
    private Boolean isResent;
    private Long resourceVersion;
    private Boolean deleted;
    private Double discount;
    private Double subTotal;
    private Double total;
    private String currencyCode;
    private String baseCurrencyCode;
    private Boolean isGifted;
    private String orderLineItems; // JSON string, links to item_entity via entity_id
    // Add more fields as needed

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getShippingAddress() {
        return shippingAddress;
    }
    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    public String getBillingAddress() {
        return billingAddress;
    }
    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getPriceType() { return priceType; }
    public void setPriceType(String priceType) { this.priceType = priceType; }
    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
    public Date getShippingDate() { return shippingDate; }
    public void setShippingDate(Date shippingDate) { this.shippingDate = shippingDate; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Double getTax() { return tax; }
    public void setTax(Double tax) { this.tax = tax; }
    public Double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(Double amountPaid) { this.amountPaid = amountPaid; }
    public Double getAmountAdjusted() { return amountAdjusted; }
    public void setAmountAdjusted(Double amountAdjusted) { this.amountAdjusted = amountAdjusted; }
    public Double getRefundableCreditsIssued() { return refundableCreditsIssued; }
    public void setRefundableCreditsIssued(Double refundableCreditsIssued) { this.refundableCreditsIssued = refundableCreditsIssued; }
    public Double getRefundableCredits() { return refundableCredits; }
    public void setRefundableCredits(Double refundableCredits) { this.refundableCredits = refundableCredits; }
    public Double getRoundingAdjustement() { return roundingAdjustement; }
    public void setRoundingAdjustement(Double roundingAdjustement) { this.roundingAdjustement = roundingAdjustement; }
    public Date getPaidOn() { return paidOn; }
    public void setPaidOn(Date paidOn) { this.paidOn = paidOn; }
    public Double getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(Double exchangeRate) { this.exchangeRate = exchangeRate; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    public Boolean getIsResent() { return isResent; }
    public void setIsResent(Boolean isResent) { this.isResent = isResent; }
    public Long getResourceVersion() { return resourceVersion; }
    public void setResourceVersion(Long resourceVersion) { this.resourceVersion = resourceVersion; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    public Double getSubTotal() { return subTotal; }
    public void setSubTotal(Double subTotal) { this.subTotal = subTotal; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getBaseCurrencyCode() { return baseCurrencyCode; }
    public void setBaseCurrencyCode(String baseCurrencyCode) { this.baseCurrencyCode = baseCurrencyCode; }
    public Boolean getIsGifted() { return isGifted; }
    public void setIsGifted(Boolean isGifted) { this.isGifted = isGifted; }
    public String getOrderLineItems() { return orderLineItems; }
    public void setOrderLineItems(String orderLineItems) { this.orderLineItems = orderLineItems; }
}
