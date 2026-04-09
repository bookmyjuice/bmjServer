package com.bookmyjuice.models.dto;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class OrderResponse {
    public String id;
    public String customerId;
    public String status;
    public Date createdAt;
    public Map<String, Object> shippingAddress;
    public Map<String, Object> billingAddress;
    public String documentNumber;
    public String invoiceId;
    public String paymentStatus;
    public String orderType;
    public String priceType;
    public Date orderDate;
    public Date shippingDate;
    public String createdBy;
    public Double tax;
    public Double amountPaid;
    public Double amountAdjusted;
    public Double refundableCreditsIssued;
    public Double refundableCredits;
    public Double roundingAdjustement;
    public Date paidOn;
    public Double exchangeRate;
    public Date updatedAt;
    public Boolean isResent;
    public Long resourceVersion;
    public Boolean deleted;
    public Double discount;
    public Double subTotal;
    public Double total;
    public String currencyCode;
    public String baseCurrencyCode;
    public Boolean isGifted;
    public List<Object> orderLineItems;
}
