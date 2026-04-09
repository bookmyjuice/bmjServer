package com.bookmyjuice.services;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.entities.CustomerEntity;
import com.bookmyjuice.models.entities.SubscriptionEntity;
import com.bookmyjuice.models.entities.SubscriptionItemEntity;
import com.bookmyjuice.models.mappers.CustomerMapper;
import com.bookmyjuice.models.mappers.SubscriptionItemMapper;
import com.bookmyjuice.models.mappers.SubscriptionMapper;
import com.bookmyjuice.repository.CustomerRepository;
import com.bookmyjuice.repository.SubscriptionEntityRepository;
import com.bookmyjuice.repository.SubscriptionItemEntityRepository;
import com.chargebee.models.Event;

@Service
public class SubscriptionService {

    private static final Logger logger = LogManager.getLogger(SubscriptionService.class);

    @Autowired
    private SubscriptionItemEntityRepository subscriptionItemRepository;

    @Autowired
    private SubscriptionEntityRepository subscriptionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Transactional
    public boolean saveSubscriptions(Event e) {
        if (e == null || e.content() == null || e.content().subscription() == null) {
            logger.error("Invalid event data received: {}", e);
            throw new IllegalArgumentException("Invalid event data");
        }

        logger.info("Processing subscription creation event: {}", e.content().subscription().id());

        // Map the subscription from the event
        SubscriptionEntity subscription = SubscriptionMapper.toEntity(e.content().subscription());
        logger.debug("Mapped subscription entity: {}", subscription);

        // 1. Handle customer
        CustomerEntity customer = customerRepository.findById(e.content().customer().id())
                .orElseGet(() -> {
                    logger.info("Customer not found, creating new customer: {}", e.content().customer().id());
                    CustomerEntity newCustomer = CustomerMapper.toEntity(e.content().customer());
                    return customerRepository.save(newCustomer);
                });
        // Always update addresses from event if present
        CustomerEntity eventCustomer = CustomerMapper.toEntity(e.content().customer());
        if (eventCustomer.getBillingAddress() != null) {
            customer.setBillingAddress(eventCustomer.getBillingAddress());
        }
        if (eventCustomer.getShippingAddress() != null) {
            customer.setShippingAddress(eventCustomer.getShippingAddress());
        }
        // customerRepository.save(customer);
        subscription.setCustomer(customer);

        // Check if the subscription already exists
        if (subscriptionRepository.existsById(subscription.getId())) {
            logger.info("Subscription already exists, updating: {}", subscription.getId());
            SubscriptionEntity existingSubscription = subscriptionRepository.findById(subscription.getId())
                    .orElseThrow(() -> new IllegalStateException("Subscription not found during update"));
            existingSubscription.setBillingPeriod(subscription.getBillingPeriod());
            existingSubscription.setBillingPeriodUnit(subscription.getBillingPeriodUnit());
            existingSubscription.setStatus(subscription.getStatus());
            existingSubscription.setCustomer(customer);
            subscription = existingSubscription;
        }
        subscriptionRepository.save(subscription);
        // Handle subscription items
        if (e.content().subscription().subscriptionItems() != null) {
            final SubscriptionEntity finalSubscription = subscription;
            e.content().subscription().subscriptionItems().forEach(item -> {
                SubscriptionItemEntity subscriptionItem = SubscriptionItemMapper.toEntity(item);
                logger.debug("Mapped subscription item entity: {}", subscriptionItem);

                subscriptionItem.setSubscription(finalSubscription); // Link the subscription to the item
                subscriptionItemRepository.findById(subscriptionItem.getItemPriceId())
                        .ifPresentOrElse(existingItem -> {
                            logger.info("Updating existing subscription item: {}", subscriptionItem.getItemPriceId());
                            existingItem.setQuantity(subscriptionItem.getQuantity());
                            existingItem.setQuantityInDecimal(subscriptionItem.getQuantityInDecimal());
                            existingItem.setUnitPrice(subscriptionItem.getUnitPrice());
                            existingItem.setUnitPriceInDecimal(subscriptionItem.getUnitPriceInDecimal());
                            existingItem.setAmount(subscriptionItem.getAmount());
                            existingItem.setAmountInDecimal(subscriptionItem.getAmountInDecimal());
                            existingItem.setCurrentTermStart(subscriptionItem.getCurrentTermStart());
                            existingItem.setCurrentTermEnd(subscriptionItem.getCurrentTermEnd());
                            existingItem.setNextBillingAt(subscriptionItem.getNextBillingAt());
                            existingItem.setBillingPeriod(subscriptionItem.getBillingPeriod());
                            existingItem.setBillingPeriodUnit(subscriptionItem.getBillingPeriodUnit());
                            existingItem.setFreeQuantity(subscriptionItem.getFreeQuantity());
                            existingItem.setFreeQuantityInDecimal(subscriptionItem.getFreeQuantityInDecimal());
                            existingItem.setTrialEnd(subscriptionItem.getTrialEnd());
                            existingItem.setBillingCycles(subscriptionItem.getBillingCycles());
                            existingItem.setServicePeriodDays(subscriptionItem.getServicePeriodDays());
                            existingItem.setChargeOnEvent(subscriptionItem.getChargeOnEvent());
                            existingItem.setChargeOnce(subscriptionItem.getChargeOnce());
                            existingItem.setChargeOnOption(subscriptionItem.getChargeOnOption());
                            existingItem.setProrationType(subscriptionItem.getProrationType());
                            existingItem.setUsageAccumulationResetFrequency(subscriptionItem.getUsageAccumulationResetFrequency());
                            subscriptionItemRepository.save(existingItem);
                        }, () -> {
                            logger.info("Saving new subscription item: {}", subscriptionItem.getItemPriceId());
                            subscriptionItemRepository.save(subscriptionItem);
                            // catch (Exception ex) {
                            //     logger.error("Error saving subscription item: {}", ex.getMessage(), ex);
                            // }
                        });
            });
        }

        // Save the subscription
        logger.info("Subscription saved successfully: {}", subscription.getId());
        return true;
    }

    public List<SubscriptionEntity> findByCustomerId(String id) {
        logger.info("Fetching subscriptions for customer: {}", id);
        return subscriptionRepository.findByCustomer(customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with id: " + id)));
    }

    public boolean existsByCustomerId(String id) {
        logger.info("Checking if subscription exists for customer: {}", id);
        return subscriptionRepository.existsByCustomerId(id);
    }

    public boolean updateSubscription(Event e) {
        if (e == null || e.content() == null || e.content().subscription() == null) {
            logger.error("Invalid event data received for update: {}", e);
            return false;
        }

        logger.info("Processing subscription update event: {}", e.content().subscription().id());

        String subscriptionId = e.content().subscription().id();
        SubscriptionEntity subscription;
        if (subscriptionRepository.existsById(subscriptionId)) {
            subscription = SubscriptionMapper.toEntity(e.content().subscription());
        } else {
            logger.warn("Subscription not found for update: {}. Creating new subscription.", subscriptionId);
            subscription = SubscriptionMapper.toEntity(e.content().subscription());
            // Optionally, handle nested entities if needed
        }
        subscriptionRepository.save(subscription);
        logger.info("Subscription updated or created successfully: {}", subscription.getId());
        return true;
    }

    @Transactional
    public boolean reactivateSubscription(Event e) {
        if (e == null || e.content() == null || e.content().subscription() == null) {
            logger.error("Invalid event data received for reactivating subscription: {}", e);
            throw new IllegalArgumentException("Invalid event data");
        }

        logger.info("Processing subscription reactivation event: {}", e.content().subscription().id());

        String subscriptionId = e.content().subscription().id();
        SubscriptionEntity subscription;
        if (subscriptionRepository.existsById(subscriptionId)) {
            subscription = subscriptionRepository.findById(subscriptionId).get();
            subscription.setStatus("active");
        } else {
            logger.warn("Subscription not found for reactivation: {}. Creating new subscription.", subscriptionId);
            subscription = SubscriptionMapper.toEntity(e.content().subscription());
            subscription.setStatus("active");
        }
        subscriptionRepository.save(subscription);
        logger.info("Subscription reactivated or created successfully: {}", subscription.getId());
        return true;
    }

    @Transactional
    public boolean renewSubscription(Event e) {
        if (e == null || e.content() == null || e.content().subscription() == null) {
            logger.error("Invalid event data received for renewing subscription: {}", e);
            throw new IllegalArgumentException("Invalid event data");
        }

        logger.info("Processing subscription renewal event: {}", e.content().subscription().id());

        String subscriptionId = e.content().subscription().id();
        SubscriptionEntity subscription;
        CustomerEntity customer = customerRepository.findById(e.content().customer().id())
                .orElseGet(() -> {
                    logger.info("Customer not found, creating new customer: {}", e.content().customer().id());
                    CustomerEntity newCustomer = CustomerMapper.toEntity(e.content().customer());
                    return customerRepository.save(newCustomer);
                });

        // Always update addresses from event if present
        CustomerEntity eventCustomer = CustomerMapper.toEntity(e.content().customer());
        if (eventCustomer.getBillingAddress() != null) {
            customer.setBillingAddress(eventCustomer.getBillingAddress());
        }
        if (eventCustomer.getShippingAddress() != null) {
            customer.setShippingAddress(eventCustomer.getShippingAddress());
        }
        // customerRepository.save(customer);

        if (subscriptionRepository.existsById(subscriptionId)) {
            subscription = subscriptionRepository.findById(subscriptionId).get();
            subscription.setRenewed(true);
            // Ensure customer is linked
            subscription.setCustomer(customer);
        } else {
            logger.warn("Subscription not found for renewal: {}. Creating new subscription.", subscriptionId);
            subscription = SubscriptionMapper.toEntity(e.content().subscription());
            subscription.setRenewed(true);
            subscription.setCustomer(customer);
        }
        subscriptionRepository.save(subscription);

        // Handle subscription items
        if (e.content().subscription().subscriptionItems() != null) {
            final SubscriptionEntity finalSubscription = subscription;
            e.content().subscription().subscriptionItems().forEach(item -> {
                SubscriptionItemEntity subscriptionItem = SubscriptionItemMapper.toEntity(item);
                logger.debug("Mapped subscription item entity: {}", subscriptionItem);

                subscriptionItem.setSubscription(finalSubscription); // Link the subscription to the item
                subscriptionItemRepository.findById(subscriptionItem.getItemPriceId())
                        .ifPresentOrElse(existingItem -> {
                            // If not linked, link it
                            if (existingItem.getSubscription() == null
                                    || !existingItem.getSubscription().getId().equals(finalSubscription.getId())) {
                                existingItem.setSubscription(finalSubscription);
                            }
                            // Update fields
                            existingItem.setQuantity(subscriptionItem.getQuantity());
                            existingItem.setQuantityInDecimal(subscriptionItem.getQuantityInDecimal());
                            existingItem.setUnitPrice(subscriptionItem.getUnitPrice());
                            existingItem.setUnitPriceInDecimal(subscriptionItem.getUnitPriceInDecimal());
                            existingItem.setAmount(subscriptionItem.getAmount());
                            existingItem.setAmountInDecimal(subscriptionItem.getAmountInDecimal());
                            existingItem.setCurrentTermStart(subscriptionItem.getCurrentTermStart());
                            existingItem.setCurrentTermEnd(subscriptionItem.getCurrentTermEnd());
                            existingItem.setNextBillingAt(subscriptionItem.getNextBillingAt());
                            existingItem.setBillingPeriod(subscriptionItem.getBillingPeriod());
                            existingItem.setBillingPeriodUnit(subscriptionItem.getBillingPeriodUnit());
                            existingItem.setFreeQuantity(subscriptionItem.getFreeQuantity());
                            existingItem.setFreeQuantityInDecimal(subscriptionItem.getFreeQuantityInDecimal());
                            existingItem.setTrialEnd(subscriptionItem.getTrialEnd());
                            existingItem.setBillingCycles(subscriptionItem.getBillingCycles());
                            existingItem.setServicePeriodDays(subscriptionItem.getServicePeriodDays());
                            existingItem.setChargeOnEvent(subscriptionItem.getChargeOnEvent());
                            existingItem.setChargeOnce(subscriptionItem.getChargeOnce());
                            existingItem.setChargeOnOption(subscriptionItem.getChargeOnOption());
                            existingItem.setProrationType(subscriptionItem.getProrationType());
                            existingItem.setUsageAccumulationResetFrequency(subscriptionItem.getUsageAccumulationResetFrequency());
                            subscriptionItemRepository.save(existingItem);
                        }, () -> {
                            logger.info("Saving new subscription item: {}", subscriptionItem.getItemPriceId());
                            subscriptionItemRepository.save(subscriptionItem);
                        });
            });
        }

        logger.info("Subscription renewed or created successfully: {}", subscription.getId());
        return true;
    }

    public  Optional<SubscriptionEntity> findBySubscription(String id) {
        logger.info("Fetching subscription items for subscription: {}", id);
        return subscriptionRepository.findById(id);
    }   
}
