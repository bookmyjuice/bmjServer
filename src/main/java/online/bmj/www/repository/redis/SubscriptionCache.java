// package online.bmj.www.repository.redis;

// import java.util.Optional;
// import java.util.UUID;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.CachePut;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.stereotype.Component;

// import online.bmj.www.entities.SubscriptionEntity;
// import online.bmj.www.repository.jpa.SubscriptionsRepository;

// @Component
// public class SubscriptionCache {

//     @Autowired
//     public SubscriptionsRepository subscriptionRepository;


//     /**
//      * Retrieves a subscription from the cache. If not present, fetches from DB and caches it.
//      * 
//      * @param subscriptionId the unique subscription ID
//      * @return SubscriptionEntity if found, otherwise null
//      */
//     @Cacheable(value = "subscriptions", key = "#subscriptionId")
//     public SubscriptionEntity getSubscription(UUID subscriptionId) {
//         Optional<SubscriptionEntity> subscription = subscriptionRepository.findById(subscriptionId);
//         return subscription.orElse(null);
//     }

//     /**
//      * Adds a new subscription to the cache.
//      * 
//      * @param subscription the SubscriptionEntity to cache
//      * @return the cached SubscriptionEntity
//      */
//     @CachePut(value = "subscriptions", key = "#subscription.subscriptionId")
//     public SubscriptionEntity cacheSubscription(SubscriptionEntity subscription) {
//         return subscription;
//     }

//     /**
//      * Updates an existing subscription in the cache.
//      * 
//      * @param subscription the updated SubscriptionEntity
//      * @return the updated SubscriptionEntity
//      */
//     @CachePut(value = "subscriptions", key = "#subscription.subscriptionId")
//     public SubscriptionEntity updateSubscription(SubscriptionEntity subscription) {
//         return subscription;
//     }

//     /**
//      * Removes a subscription from the cache.
//      * 
//      * @param subscriptionId the ID of the subscription to remove
//      */
//     @CacheEvict(value = "subscriptions", key = "#subscriptionId")
//     public void evictSubscription(String subscriptionId) {
//         // No return value, simply removes from cache
//     }

//     /**
//      * Clears all subscriptions from the cache.
//      */
//     @CacheEvict(value = "subscriptions", allEntries = true)
//     public void evictAllSubscriptions() {
//         // No return value, clears entire cache
//     }
// }
