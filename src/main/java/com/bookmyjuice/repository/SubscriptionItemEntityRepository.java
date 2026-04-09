package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.SubscriptionItemEntity;

@Repository
public interface SubscriptionItemEntityRepository extends JpaRepository<SubscriptionItemEntity, String> {
    java.util.List<SubscriptionItemEntity> findBySubscription_Id(String subscriptionId);
}