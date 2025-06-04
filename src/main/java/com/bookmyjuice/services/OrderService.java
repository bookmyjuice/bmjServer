package com.bookmyjuice.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bookmyjuice.repository.OrderRepository;
import com.bookmyjuice.models.entities.OrderEntity;
import com.bookmyjuice.models.mappers.OrderMapper;
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
}
