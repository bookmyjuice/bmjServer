package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.SubscriptionItemEntity;

@Repository
public interface SubscriptionItemRepository extends JpaRepository<SubscriptionItemEntity, String> {
    // Additional custom query methods can be added here if needed
}