package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.CustomerEntity;
import com.bookmyjuice.models.SubscriptionEntity;
import java.util.List;



@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {
    // SubscriptionEntity findByCustomerId(String userId); // Assuming you have a userId field in Subscription
    // boolean existsByCustomerId(String userId); // Assuming you have a userId field in Subscription
    // boolean existsBy(String id); // Assuming you have a userId field in Subscription
    // boolean existsByFamilyId(String familyId); // Assuming you have a familyId field in Subscription
    boolean existsByCustomerId(String Id);
    List<SubscriptionEntity> findByCustomer(CustomerEntity customer);
}
