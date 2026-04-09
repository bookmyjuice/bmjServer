package com.bookmyjuice.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.entities.OrderEntity;
import com.bookmyjuice.models.mappers.OrderMapper;
import com.bookmyjuice.repository.OrderRepository;
import com.chargebee.models.Event;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    public boolean saveOrUpdateOrder(Event event) {
        var order = event.content().order();
        OrderEntity entity = orderRepository.findById(order.id())
            .orElseGet(() -> OrderMapper.toEntity(order));
        OrderMapper.toEntity(order, entity);
        orderRepository.save(entity);
        return true;
    }

    public boolean deleteOrder(Event event) {
        var order = event.content().order();
        if (orderRepository.existsById(order.id())) {
            orderRepository.deleteById(order.id());
        }
        return true;
    }

    public List<com.bookmyjuice.models.dto.OrderResponse> fetchOrders() {
        List<OrderEntity> entities = orderRepository.findAll();
        List<com.bookmyjuice.models.dto.OrderResponse> responses = new java.util.ArrayList<>();
        for (OrderEntity entity : entities) {
            responses.add(com.bookmyjuice.models.mappers.OrderResponseMapper.toResponse(entity));
        }
        return responses;
    }

    public List<com.bookmyjuice.models.dto.OrderResponse> fetchOrdersByCustomerId(String customerId) {
        List<OrderEntity> entities = orderRepository.findByCustomerId(customerId);
        List<com.bookmyjuice.models.dto.OrderResponse> responses = new java.util.ArrayList<>();
        for (OrderEntity entity : entities) {
            responses.add(com.bookmyjuice.models.mappers.OrderResponseMapper.toResponse(entity));
        }
        return responses;
    }
}
