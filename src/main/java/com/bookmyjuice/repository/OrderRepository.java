package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.OrderEntity;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    // Add custom queries if needed
}
