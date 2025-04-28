package com.bezkoder.springjwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bezkoder.springjwt.models.SubscriptionEntity;

// import com.bezkoder.springjwt.models.Subscription;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, String> {
    SubscriptionEntity findByCustomerId(String userId); // Assuming you have a userId field in Subscription
    boolean existsByCustomerId(String userId); // Assuming you have a userId field in Subscription
    // Subscription findByPlanId(Long planId); // Assuming you have a planId field in Subscription
    // Subscription findByPlanItemPriceId(String planItemPriceId); // Assuming you have a planItemPriceId field in Subscription
    // void deleteByUserId(String userId); // Assuming you want to delete by user ID
    // void deleteByPlanId(String planId); // Assuming you want to delete by plan ID
    // void deleteByPlanItemPriceId(String planItemPriceId); // Assuming you want to delete by plan item price ID
    
}
