package com.bezkoder.springjwt.security.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bezkoder.springjwt.models.SubscriptionEntity;
import com.bezkoder.springjwt.models.SubscriptionMapper;
import com.bezkoder.springjwt.repository.SubscriptionRepository;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public boolean saveSubscriptions(Map<String,Object> e) {
        if (e == null) {
            return false;
        }
        else{
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) e.get("content");
        @SuppressWarnings("unchecked")
        Map<String, Object> subscriptionObject = (Map<String, Object>) content.get("subscription");
        
        SubscriptionEntity subscription = SubscriptionMapper.toEntity(subscriptionObject);
       
        if (subscriptionRepository.existsById(subscription.getId())) {
            return false;
        } else {
            subscriptionRepository.save(subscription);
            return true;
        }}
    }

    public String getSubscriptionDetails() {
        // Logic to retrieve subscription details
        return "Subscription details";
    }

    public boolean existsById(String id) {
        // Logic to check if subscription exists by ID
        return subscriptionRepository.existsByCustomerId(id);
    }
    public String findSubscriptionByCustomerId(String id) {
        // Logic subscription Id by customer ID
        return subscriptionRepository.findByCustomerId(id).getId();
    }

    public String updateSubscription(String subscriptionId, String newDetails) {
        // Logic to update subscription
        return "Updated subscription with ID: " + subscriptionId + " to new details: " + newDetails;

    }
}
