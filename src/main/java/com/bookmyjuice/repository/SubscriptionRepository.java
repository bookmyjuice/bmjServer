package com.bookmyjuice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.models.entities.SubscriptionEntity;



@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {
    // SubscriptionEntity findByCustomerId(String userId); // Assuming you have a userId field in Subscription
    // boolean existsByCustomerId(String userId); // Assuming you have a userId field in Subscription
    // boolean existsBy(String id); // Assuming you have a userId field in Subscription
    // boolean existsByFamilyId(String familyId); // Assuming you have a familyId field in Subscription
    boolean existsByCustomerId(String Id);
    List<SubscriptionEntity> findByCustomer(CustomerEntity customer);
}
