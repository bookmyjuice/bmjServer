package com.bookmyjuice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookmyjuice.models.CustomerEntity;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {
    CustomerEntity findByCustomerId(String customerId); // Assuming you have a customerId field in Customer
    boolean existsByCustomerId(String customerId); // Assuming you have a customerId field in Customer
    // CustomerEntity findByPlanId(Long planId); // Assuming you have a planId field in Customer
    // CustomerEntity findByPlanItemPriceId(String planItemPriceId); // Assuming you have a planItemPriceId field in Customer
    // void deleteByUserId(String userId); // Assuming you want to delete by user ID
    // void deleteByPlanId(String planId); // Assuming you want to delete by plan ID
    // void deleteByPlanItemPriceId(String planItemPriceId); // Assuming you want to delete by plan item price ID
}
