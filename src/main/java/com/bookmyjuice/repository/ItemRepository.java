package com.bookmyjuice.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.ItemEntity;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, String> {
    @Query("SELECT i FROM ItemEntity i WHERE i.subscription.id = :subscriptionId")
    List<ItemEntity> findBySubscription(@Param("subscriptionId") String subscriptionId);
}