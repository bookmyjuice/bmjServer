package com.bookmyjuice.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookmyjuice.models.CustomerMapper;
import com.bookmyjuice.models.SubscriptionEntity;
import com.bookmyjuice.models.SubscriptionMapper;
import com.bookmyjuice.repository.CustomerRepository;
import com.bookmyjuice.repository.SubscriptionRepository;
import com.chargebee.models.Event;

@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    public boolean saveSubscriptions(Event e) {
        if (e == null) {
            return false;
        }
        else{
        SubscriptionEntity subscription = SubscriptionMapper.toEntity(e.content().subscription());
        // subscription.setCustomer(customerRepository.findById(subscription.getCustomer().getId())
        //         .orElseThrow(() -> customerRepository.save(e.customer())));
        // // Check if the subscription already exists
       
        if (subscriptionRepository.existsById(subscription.getId())) {
            if (!customerRepository.existsById(e.content().customer().id())) {
                customerRepository.save(CustomerMapper.toEntity(e.content().customer()));
                return true;
            } else {
                // If the customer exists, we can update the subscription
                subscriptionRepository.save(subscription);
                return true;
            }
        } else {
            subscriptionRepository.save(subscription);
            return true;
        }}
    }

    public String getSubscriptionDetails() {
        return "Subscription details";
    }

    public List<SubscriptionEntity> findByCustomerId(String id) {
        return subscriptionRepository.findByCustomer(customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id)));
    }
    public boolean existsByCustomerId(String id) {
        return subscriptionRepository.existsByCustomerId(id);
    }
    // public String findSubscriptionByCustomerId(String id) {
    //     return subscriptionRepository.findById(id) //////////???????????
    //             .map(SubscriptionEntity::getId)
    //             .orElse(null);
    // }

    public boolean updateSubscription(Event e) {
        // Logic to update subscription
        if (e == null) {
            return false;
        }
        else{
        SubscriptionEntity subscription = SubscriptionMapper.toEntity(e.content().subscription());
        if (subscriptionRepository.existsById(subscription.getId())) {
            return false;
        } else {
            subscriptionRepository.save(subscription);
            return true;
        }}
    }
}
