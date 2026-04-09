package com.bookmyjuice.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.entities.OrderEntity;
import com.bookmyjuice.repository.OrderRepository;
import com.chargebee.ListResult;
import com.chargebee.models.Order;

/**
 * Service for managing orders via Chargebee API
 * Handles fetching order history and details
 */
@Service
public class OrderApiService {

    private static final Logger logger = LoggerFactory.getLogger(OrderApiService.class);

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Get customer orders from Chargebee API
     */
    public List<Map<String, Object>> getCustomerOrders(String customerId) throws Exception {
        logger.info("Fetching orders for customer: {}", customerId);
        List<Map<String, Object>> orders = new ArrayList<>();

        try {
            ListResult listResult = Order.list().request();
            
            for (ListResult.Entry entry : listResult) {
                Order order = entry.order();
                if (order != null && customerId.equals(order.customerId())) {
                    Map<String, Object> orderMap = mapOrderToResponse(order);
                    orders.add(orderMap);
                }
            }
            logger.info("Successfully fetched {} orders for customer: {}", orders.size(), customerId);
        } catch (Exception e) {
            logger.error("Error fetching orders for customer {}: {}", customerId, e.getMessage(), e);
            throw new Exception("Failed to fetch orders: " + e.getMessage(), e);
        }

        return orders;
    }

    /**
     * Get specific order details from Chargebee
     */
    public Map<String, Object> getOrderDetails(String orderId) throws Exception {
        logger.info("Fetching order details: {}", orderId);

        try {
            com.chargebee.Result result = Order.retrieve(orderId).request();
            Order order = result.order();
            
            return mapOrderToResponse(order);
        } catch (Exception e) {
            logger.error("Error fetching order {}: {}", orderId, e.getMessage(), e);
            throw new Exception("Failed to fetch order: " + e.getMessage(), e);
        }
    }

    /**
     * Get local customer orders from database
     */
    public List<OrderEntity> getLocalCustomerOrders(String customerId) {
        logger.info("Fetching local orders for customer: {}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    /**
     * Get specific local order
     */
    public Optional<OrderEntity> getLocalOrder(String orderId) {
        logger.info("Fetching local order: {}", orderId);
        return orderRepository.findById(orderId);
    }

    /**
     * Update order status in local database
     */
    @Transactional
    public OrderEntity updateOrderStatus(String orderId, String status) {
        logger.info("Updating order {} status to: {}", orderId, status);
        
        Optional<OrderEntity> order = orderRepository.findById(orderId);
        if (order.isPresent()) {
            OrderEntity entity = order.get();
            entity.setStatus(status);
            return orderRepository.save(entity);
        }
        return null;
    }

    /**
     * Get all orders (admin endpoint)
     */
    public List<OrderEntity> getAllOrders() {
        logger.info("Fetching all orders");
        return orderRepository.findAll();
    }

    /**
     * Map Chargebee order to response DTO
     */
    private Map<String, Object> mapOrderToResponse(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", order.id());
        map.put("customerId", order.customerId());
        map.put("status", order.status().toString());
        map.put("total", order.total());
        map.put("amountPaid", order.amountPaid());
        map.put("createdAt", order.createdAt());
        return map;
    }
}
