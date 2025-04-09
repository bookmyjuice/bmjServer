// package online.bmj.www.services;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import com.chargebee.Result;
// import com.chargebee.exceptions.OperationFailedException;
// import com.chargebee.models.Subscription;

// import online.bmj.www.entities.SubscriptionEntity;
// import online.bmj.www.repository.jpa.CustomerRepository;
// import online.bmj.www.repository.jpa.SubscriptionsRepository;
// import online.bmj.www.repository.redis.SubscriptionCache;

// @Service
// public class SubscriptionService {

//     @Autowired
//     private SubscriptionsRepository subscriptionsRepository;

//     @Autowired
//     private CustomerRepository customerRepository;

//     @Autowired
//     private SubscriptionCache subscriptionCache;

//     @Transactional
//     public SubscriptionEntity createSubscription(String customerId, String planId) {
//         try {
//             Result result = Subscription.create().id(generateSubscriptionId())
//                 .customerId(customerId)
//                 .planId(planId)
//                 .request();

//             Subscription cbSub = result.subscription();
//             SubscriptionEntity subscriptionEntity = convertToEntity(cbSub);
//             subscriptionCache.cacheSubscription(subscriptionEntity); // Cache the subscription
//             return subscriptionEntity;

//         } catch (OperationFailedException e) {
//             throw new RuntimeException("Subscription creation failed", e);
//         } catch (Exception e) {
//             throw new RuntimeException("Exception:", e);
//         }
//     }

//     private SubscriptionEntity convertToEntity(Subscription cbSub) {
//         SubscriptionEntity subscription = new SubscriptionEntity();
//         subscription.setChargebeeId(cbSub.id());
//         subscription.setPlanId(cbSub.planId());
//         subscription.setStatus(SubscriptionEntity.SubscriptionStatus.valueOf(cbSub.status().name().toUpperCase()));

//         customerRepository.findByCustomerId(cbSub.customerId()).ifPresentOrElse(
//             customer -> subscription.setCustomer(customer),
//             () -> {
//                 throw new RuntimeException("CustomerId not found");
//             }
//         );

//         subscription.setNextBillingDate(cbSub.nextBillingAt().toLocalDateTime());
//         subscriptionsRepository.save(subscription);
//         return subscription;
//     }

//     private String generateSubscriptionId() {
//         return String.valueOf(subscriptionsRepository.findAll().size() + 500);
//     }

//     public Subscription cancelSubscription(String subscriptionId) throws Exception {
//         Subscription subscription = Subscription.cancel(subscriptionId).request().subscription();
//         subscriptionCache.evictSubscription(subscriptionId); // Evict from cache
//         return subscription;
//     }

//     public Subscription pauseSubscription(String subscriptionId) throws Exception {
//         Subscription subscription = Subscription.pause(subscriptionId).request().subscription();
//         subscriptionCache.evictSubscription(subscriptionId); // Evict from cache
//         return subscription;
//     }

//     public Subscription getSubscription(String subscriptionId) throws Exception {
//         return Subscription.retrieve(subscriptionId).request().subscription();
//     }

//     public Subscription reactivateSubscription(String subscriptionId) throws Exception {
//         Subscription subscription = Subscription.reactivate(subscriptionId).request().subscription();
//         subscriptionCache.evictSubscription(subscriptionId); // Evict from cache
//         return subscription;
//     }
// }