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
    
    @Query("SELECT i FROM ItemEntity i WHERE i.type = 'CHARGE' AND i.status = 'ACTIVE' AND i.archived = false AND " +
           "(LOWER(i.name) LIKE %:searchTerm% OR LOWER(i.productFamilyId) LIKE %:searchTerm%)")
    List<ItemEntity> findJuiceItems(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT i FROM ItemEntity i WHERE i.type = 'CHARGE' AND i.status = 'ACTIVE' AND i.archived = false")
    List<ItemEntity> findAllActiveChargeItems();
}