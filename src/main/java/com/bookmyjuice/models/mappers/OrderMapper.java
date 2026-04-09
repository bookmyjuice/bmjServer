package com.bookmyjuice.models.mappers;

import java.util.Date;
import java.util.List;

import com.bookmyjuice.models.entities.OrderEntity;
import com.chargebee.models.Order;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderMapper {
    public static OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.id());
        entity.setCustomerId(order.customerId());
        entity.setStatus(order.status() != null ? order.status().toString() : null);
        entity.setCreatedAt(order.createdAt() != null ? new Date(order.createdAt().getTime()) : null);
        entity.setDocumentNumber(order.documentNumber());
        entity.setInvoiceId(order.invoiceId());
        entity.setPaymentStatus(order.paymentStatus() != null ? order.paymentStatus().name() : null);
        entity.setOrderType(order.orderType() != null ? order.orderType().name() : null);
        entity.setPriceType(order.priceType() != null ? order.priceType().name() : null);
        entity.setOrderDate(order.orderDate() != null ? new Date(order.orderDate().getTime()) : null);
        entity.setShippingDate(order.shippingDate() != null ? new Date(order.shippingDate().getTime()) : null);
        entity.setCreatedBy(order.createdBy());
        entity.setTax(order.tax() != null ? order.tax().doubleValue() : null);
        entity.setAmountPaid(order.amountPaid() != null ? order.amountPaid().doubleValue() : null);
        entity.setAmountAdjusted(order.amountAdjusted() != null ? order.amountAdjusted().doubleValue() : null);
        entity.setRefundableCreditsIssued(order.refundableCreditsIssued() != null ? order.refundableCreditsIssued().doubleValue() : null);
        entity.setRefundableCredits(order.refundableCredits() != null ? order.refundableCredits().doubleValue() : null);
        entity.setRoundingAdjustement(order.roundingAdjustement() != null ? order.roundingAdjustement().doubleValue() : null);
        entity.setPaidOn(order.paidOn() != null ? new Date(order.paidOn().getTime()) : null);
        // entity.setExchangeRate(order.exchangeRate() != null ? order.exchangeRate().doubleValue() : null);
        entity.setUpdatedAt(order.updatedAt() != null ? new Date(order.updatedAt().getTime()) : null);
        entity.setIsResent(order.isResent());
        entity.setResourceVersion(order.resourceVersion());
        entity.setDeleted(order.deleted());
        entity.setDiscount(order.discount() != null ? order.discount().doubleValue() : null);
        entity.setSubTotal(order.subTotal() != null ? order.subTotal().doubleValue() : null);
        entity.setTotal(order.total() != null ? order.total().doubleValue() : null);
        entity.setCurrencyCode(order.currencyCode());
        // entity.setBaseCurrencyCode(order.baseCurrencyCode());
        entity.setIsGifted(order.isGifted());
            ObjectMapper mapper = new ObjectMapper();
            try {
                // Store as JSON string in DB
                entity.setShippingAddress(order.shippingAddress() != null ? mapper.writeValueAsString(order.shippingAddress()) : null);
                entity.setBillingAddress(order.billingAddress() != null ? mapper.writeValueAsString(order.billingAddress()) : null);
                List<?> lineItems = order.orderLineItems();
                entity.setOrderLineItems(lineItems != null ? mapper.writeValueAsString(lineItems) : "[]");
            } catch (JsonProcessingException e) {
                entity.setShippingAddress(order.shippingAddress() != null ? order.shippingAddress().toString() : null);
                entity.setBillingAddress(order.billingAddress() != null ? order.billingAddress().toString() : null);
                entity.setOrderLineItems(order.orderLineItems() != null ? order.orderLineItems().toString() : "[]");
            }
        return entity;
    }

    public static void toEntity(Order order, OrderEntity entity) {
        entity.setCustomerId(order.customerId());
        entity.setStatus(order.status() != null ? order.status().toString() : null);
        entity.setCreatedAt(order.createdAt() != null ? new Date(order.createdAt().getTime()) : null);
            ObjectMapper mapper = new ObjectMapper();
            try {
                entity.setShippingAddress(order.shippingAddress() != null ? mapper.writeValueAsString(order.shippingAddress()) : null);
                entity.setBillingAddress(order.billingAddress() != null ? mapper.writeValueAsString(order.billingAddress()) : null);
                List<?> lineItems = order.orderLineItems();
                entity.setOrderLineItems(lineItems != null ? mapper.writeValueAsString(lineItems) : "[]");
            } catch (JsonProcessingException e) {
                entity.setShippingAddress(order.shippingAddress() != null ? order.shippingAddress().toString() : null);
                entity.setBillingAddress(order.billingAddress() != null ? order.billingAddress().toString() : null);
                entity.setOrderLineItems(order.orderLineItems() != null ? order.orderLineItems().toString() : "[]");
            }
        // Update more fields as needed
    }
}