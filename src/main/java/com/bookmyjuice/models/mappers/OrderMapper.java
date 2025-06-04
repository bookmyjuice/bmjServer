package com.bookmyjuice.models.mappers;

import com.bookmyjuice.models.entities.OrderEntity;
import com.chargebee.models.Order;
import java.util.Date;

public class OrderMapper {
    public static OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.id());
        entity.setCustomerId(order.customerId());
        entity.setStatus(order.status() != null ? order.status().toString() : null);
        entity.setCreatedAt(order.createdAt() != null ? new Date(order.createdAt().getTime()) : null);
        entity.setShippingAddress(order.shippingAddress() != null ? order.shippingAddress().toString() : null);
        entity.setBillingAddress(order.billingAddress() != null ? order.billingAddress().toString() : null);
        // Map more fields as needed
        return entity;
    }

    public static void toEntity(Order order, OrderEntity entity) {
        entity.setCustomerId(order.customerId());
        entity.setStatus(order.status() != null ? order.status().toString() : null);
        entity.setCreatedAt(order.createdAt() != null ? new Date(order.createdAt().getTime()) : null);
        entity.setShippingAddress(order.shippingAddress() != null ? order.shippingAddress().toString() : null);
        entity.setBillingAddress(order.billingAddress() != null ? order.billingAddress().toString() : null);
        // Update more fields as needed
    }
}