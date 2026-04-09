package com.bookmyjuice.models.mappers;

import com.bookmyjuice.models.entities.OrderEntity;
import com.bookmyjuice.models.dto.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class OrderResponseMapper {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static OrderResponse toResponse(OrderEntity entity) {
        OrderResponse resp = new OrderResponse();
        resp.id = entity.getId();
        resp.customerId = entity.getCustomerId();
        resp.status = entity.getStatus();
        resp.createdAt = entity.getCreatedAt();
        resp.documentNumber = entity.getDocumentNumber();
        resp.invoiceId = entity.getInvoiceId();
        resp.paymentStatus = entity.getPaymentStatus();
        resp.orderType = entity.getOrderType();
        resp.priceType = entity.getPriceType();
        resp.orderDate = entity.getOrderDate();
        resp.shippingDate = entity.getShippingDate();
        resp.createdBy = entity.getCreatedBy();
        resp.tax = entity.getTax();
        resp.amountPaid = entity.getAmountPaid();
        resp.amountAdjusted = entity.getAmountAdjusted();
        resp.refundableCreditsIssued = entity.getRefundableCreditsIssued();
        resp.refundableCredits = entity.getRefundableCredits();
        resp.roundingAdjustement = entity.getRoundingAdjustement();
        resp.paidOn = entity.getPaidOn();
        resp.exchangeRate = entity.getExchangeRate();
        resp.updatedAt = entity.getUpdatedAt();
        resp.isResent = entity.getIsResent();
        resp.resourceVersion = entity.getResourceVersion();
        resp.deleted = entity.getDeleted();
        resp.discount = entity.getDiscount();
        resp.subTotal = entity.getSubTotal();
        resp.total = entity.getTotal();
        resp.currencyCode = entity.getCurrencyCode();
        resp.baseCurrencyCode = entity.getBaseCurrencyCode();
        resp.isGifted = entity.getIsGifted();
        // Deserialize JSON string fields
        try {
            resp.shippingAddress = entity.getShippingAddress() != null ? mapper.readValue(entity.getShippingAddress(), Map.class) : null;
        } catch (Exception e) {
            resp.shippingAddress = null;
        }
        try {
            resp.billingAddress = entity.getBillingAddress() != null ? mapper.readValue(entity.getBillingAddress(), Map.class) : null;
        } catch (Exception e) {
            resp.billingAddress = null;
        }
        try {
            resp.orderLineItems = entity.getOrderLineItems() != null ? mapper.readValue(entity.getOrderLineItems(), List.class) : new ArrayList<>();
        } catch (Exception e) {
            resp.orderLineItems = new ArrayList<>();
        }
        return resp;
    }
}
