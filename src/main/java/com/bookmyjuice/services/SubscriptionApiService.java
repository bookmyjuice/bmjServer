package com.bookmyjuice.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.repository.PlanRepository;
import com.chargebee.ListResult;
import com.chargebee.Result;
import com.chargebee.models.HostedPage;
import com.chargebee.models.Plan;
import com.chargebee.models.Subscription;

/**
 * Service for managing subscriptions via Chargebee API
 * Handles creating, updating, pausing, resuming, and canceling subscriptions
 */
@Service
public class SubscriptionApiService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionApiService.class);

    @Autowired
    private PlanRepository planRepository;

    /**
     * Create a subscription hosted page for the customer to purchase a plan
     */
    public HostedPage createSubscriptionHostedPage(String customerId, String planId) throws Exception {
        logger.info("Creating subscription hosted page for customer: {} to plan: {}", customerId, planId);

        try {
            Result result = HostedPage.checkoutNew()
                    .customerId(customerId)
                    .subscriptionPlanId(planId)
                    .request();

            HostedPage hostedPage = result.hostedPage();
            logger.info("Subscription hosted page created successfully: {}", hostedPage.id());

            return hostedPage;
        } catch (Exception e) {
            logger.error("Error creating subscription hosted page: {}", e.getMessage(), e);
            throw new Exception("Failed to create subscription hosted page: " + e.getMessage(), e);
        }
    }

    /**
     * Get subscription details from Chargebee
     */
    @Transactional
    public Map<String, Object> getSubscriptionDetails(String subscriptionId) throws Exception {
        logger.info("Fetching subscription details: {}", subscriptionId);

        try {
            Result result = Subscription.retrieve(subscriptionId).request();
            Subscription subscription = result.subscription();
            
            return mapSubscriptionToResponse(subscription);
        } catch (Exception e) {
            logger.error("Error fetching subscription {}: {}", subscriptionId, e.getMessage(), e);
            throw new Exception("Failed to fetch subscription: " + e.getMessage(), e);
        }
    }

    /**
     * List all plans available in Chargebee
     */
    public List<Map<String, Object>> getAllPlans() throws Exception {
        logger.info("Fetching all subscription plans");
        List<Map<String, Object>> plans = new ArrayList<>();

        try {
            ListResult listResult = Plan.list().request();
            
            for (ListResult.Entry entry : listResult) {
                Plan plan = entry.plan();
                if (plan != null) {
                    Map<String, Object> planMap = new HashMap<>();
                    planMap.put("id", plan.id());
                    planMap.put("name", plan.name());
                    planMap.put("description", plan.description());
                    planMap.put("price", plan.price());
                    planMap.put("period", plan.period());
                    planMap.put("periodUnit", plan.periodUnit());
                    planMap.put("status", plan.status());
                    plans.add(planMap);
                }
            }
            logger.info("Successfully fetched {} plans", plans.size());
        } catch (Exception e) {
            logger.error("Error fetching plans: {}", e.getMessage(), e);
            throw new Exception("Failed to fetch plans: " + e.getMessage(), e);
        }

        return plans;
    }

    /**
     * Get pricing page URL for customer
     */
    public String getPricingPageUrl(String customerId) throws Exception {
        logger.info("Getting pricing page URL for customer: {}", customerId);

        try {
            Result result = HostedPage.checkoutNew()
                    .customerId(customerId)
                    .request();

            HostedPage hostedPage = result.hostedPage();
            logger.info("Pricing page retrieved: {}", hostedPage.url());

            return hostedPage.url();
        } catch (Exception e) {
            logger.error("Error getting pricing page: {}", e.getMessage(), e);
            throw new Exception("Failed to get pricing page: " + e.getMessage(), e);
        }
    }

    /**
     * Pause a subscription (scheduled at end of current term)
     */
    @Transactional
    public boolean pauseSubscription(String subscriptionId) throws Exception {
        logger.info("Pausing subscription: {}", subscriptionId);

        try {
            Result result = Subscription.pause(subscriptionId).request();
            Subscription subscription = result.subscription();

            logger.info("Subscription paused successfully: {} with status: {}", subscriptionId, subscription.status());
            return true;
        } catch (Exception e) {
            logger.error("Error pausing subscription {}: {}", subscriptionId, e.getMessage(), e);
            throw new Exception("Failed to pause subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Resume a paused subscription
     */
    @Transactional
    public boolean resumeSubscription(String subscriptionId) throws Exception {
        logger.info("Resuming subscription: {}", subscriptionId);

        try {
            Result result = Subscription.resume(subscriptionId).request();
            Subscription subscription = result.subscription();

            logger.info("Subscription resumed successfully: {} with status: {}", subscriptionId, subscription.status());
            return true;
        } catch (Exception e) {
            logger.error("Error resuming subscription {}: {}", subscriptionId, e.getMessage(), e);
            throw new Exception("Failed to resume subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel a subscription
     */
    @Transactional
    public boolean cancelSubscription(String subscriptionId) throws Exception {
        logger.info("Canceling subscription: {}", subscriptionId);

        try {
            Result result = Subscription.cancel(subscriptionId).request();
            Subscription subscription = result.subscription();

            logger.info("Subscription canceled successfully: {}", subscriptionId);
            return true;
        } catch (Exception e) {
            logger.error("Error canceling subscription {}: {}", subscriptionId, e.getMessage(), e);
            throw new Exception("Failed to cancel subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Map Chargebee subscription to response DTO
     */
    private Map<String, Object> mapSubscriptionToResponse(Subscription subscription) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", subscription.id());
        map.put("customerId", subscription.customerId());
        map.put("planId", subscription.planId());
        map.put("status", subscription.status().toString());
        map.put("currentTermStart", subscription.currentTermStart());
        map.put("currentTermEnd", subscription.currentTermEnd());
        map.put("nextBillingAt", subscription.nextBillingAt());
        map.put("createdAt", subscription.createdAt());
        map.put("updatedAt", subscription.updatedAt());
        return map;
    }
}
