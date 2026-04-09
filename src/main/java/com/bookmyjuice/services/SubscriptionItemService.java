package com.bookmyjuice.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.entities.SubscriptionItemEntity;
import com.bookmyjuice.repository.SubscriptionItemEntityRepository;

@Service
public class SubscriptionItemService {
    @Autowired
    private SubscriptionItemEntityRepository subscriptionItemRepository;

    @Transactional
    public SubscriptionItemEntity save(SubscriptionItemEntity item) {
        return subscriptionItemRepository.save(item);
    }

    public List<SubscriptionItemEntity> findBySubscriptionId(String subscriptionId) {
    return subscriptionItemRepository.findBySubscription_Id(subscriptionId);
    }

    public void deleteById(String itemPriceId) {
        subscriptionItemRepository.deleteById(itemPriceId);
    }
}
