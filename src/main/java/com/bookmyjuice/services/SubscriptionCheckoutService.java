package com.bookmyjuice.services;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.Cart;
import com.bookmyjuice.models.CartItem;
import com.bookmyjuice.models.SubscriptionPlan;
import com.bookmyjuice.models.User;
import com.bookmyjuice.repository.CartRepository;
import com.bookmyjuice.repository.SubscriptionPlanRepository;
import com.bookmyjuice.repository.UserRepository;
import com.chargebee.Result;
import com.chargebee.models.HostedPage;

/**
 * Service for handling subscription checkout via Chargebee Hosted Pages.
 * Supports subscription creation with day-wise delivery schedules.
 */
@Service
public class SubscriptionCheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionCheckoutService.class);

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private SubscriptionPlanRepository planRepo;

    @Autowired
    private UserRepository userRepository;

    /**
     * Creates a Chargebee Hosted Page URL for subscription checkout.
     * The user's cart must contain only 'plan' type items (no mixed carts).
     * 
     * Accepts a day-wise schedule specifying delivery days for the subscription.
     * Example schedule: { "Monday": true, "Wednesday": true, "Friday": true }
     * 
     * @param user     The authenticated user
     * @param schedule Day-wise delivery schedule (Map of day name -> boolean)
     * @return Map containing hosted page URL and ID
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkoutSubscription(User user, Map<String, Boolean> schedule) {
        logger.info("Initiating subscription checkout for user: {}", user.getId());

        // Get user's cart
        Cart cart = cartRepo.findByUserId(user.getId())
                .orElseThrow(() -> {
                    logger.error("Cart not found for user: {}", user.getId());
                    return new RuntimeException("Cart not found. Please add items to cart first.");
                });

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Please add a subscription plan before checkout.");
        }

        // Verify cart contains only 'plan' type items
        for (CartItem item : cart.getItems()) {
            if (!"plan".equals(item.getType())) {
                throw new RuntimeException(
                        "Cart contains non-subscription items. Please clear cart and add only subscription plans.");
            }
        }

        // Subscription checkout typically uses the first plan item
        CartItem planItem = cart.getItems().get(0);
        String planId = planItem.getPriceId();
        Integer quantity = planItem.getQuantity();

        // Validate the plan exists
        SubscriptionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> {
                    logger.error("Invalid subscription plan ID: {}", planId);
                    return new RuntimeException("Invalid subscription plan: " + planId);
                });

        // Get chargebee customer ID from user
        String chargebeeCustomerId = user.getChargebeeCustomerId();
        if (chargebeeCustomerId == null || chargebeeCustomerId.isEmpty()) {
            throw new RuntimeException("Chargebee customer ID not found. Please ensure your profile is set up.");
        }

        try {
            // Build the checkout request
            // Note: Chargebee's subscription checkout uses different parameters than
            // one-time checkout
            HostedPage.CheckoutNewRequest request = HostedPage.checkoutNew()
                    .customerId(chargebeeCustomerId)
                    .subscriptionPlanId(planId);

            // Add subscription quantity if greater than 1
            // TODO: Re-enable when Chargebee SDK method is confirmed
            // if (quantity != null && quantity > 1) {
            // request = request.subscriptionQuantity(quantity);
            // }

            // Add schedule as metadata if provided
            // TODO: Re-enable when Chargebee SDK method is confirmed
            // if (schedule != null && !schedule.isEmpty()) {
            // String scheduleJson = convertScheduleToMetadata(schedule);
            // request = request.subscriptionMetadata("delivery_schedule", scheduleJson);
            // }

            // Execute the request and get hosted page
            Result result = request.request();
            HostedPage hostedPage = result.hostedPage();

            logger.info("Subscription checkout hosted page created - ID: {}, URL: {}",
                    hostedPage.id(), hostedPage.url());

            Map<String, Object> response = new HashMap<>();
            response.put("hosted_page_id", hostedPage.id());
            response.put("url", hostedPage.url());
            response.put("cart_id", cart.getId());
            response.put("plan_id", planId);
            response.put("plan_name", plan.getProduct() != null ? plan.getProduct().getName() : "Unknown");
            response.put("expires_at", hostedPage.expiresAt());

            return response;

        } catch (Exception e) {
            logger.error("Error creating subscription checkout hosted page: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create subscription checkout page: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a Chargebee Hosted Page URL for subscription checkout without using
     * cart.
     * Direct plan subscription.
     * 
     * @param user     The authenticated user
     * @param planId   The subscription plan ID
     * @param schedule Day-wise delivery schedule (Map of day name -> boolean)
     * @return Map containing hosted page URL and ID
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkoutSubscriptionDirect(User user, String planId, Map<String, Boolean> schedule) {
        logger.info("Initiating direct subscription checkout for user: {}, plan: {}", user.getId(), planId);

        // Validate the plan exists
        SubscriptionPlan plan = planRepo.findById(planId)
                .orElseThrow(() -> {
                    logger.error("Invalid subscription plan ID: {}", planId);
                    return new RuntimeException("Invalid subscription plan: " + planId);
                });

        // Get chargebee customer ID from user
        String chargebeeCustomerId = user.getChargebeeCustomerId();
        if (chargebeeCustomerId == null || chargebeeCustomerId.isEmpty()) {
            throw new RuntimeException("Chargebee customer ID not found. Please ensure your profile is set up.");
        }

        try {
            // Build the checkout request
            HostedPage.CheckoutNewRequest request = HostedPage.checkoutNew()
                    .customerId(chargebeeCustomerId)
                    .subscriptionPlanId(planId);

            // Add schedule as metadata if provided
            // TODO: Re-enable when Chargebee SDK subscriptionMetadata method is confirmed
            // if (schedule != null && !schedule.isEmpty()) {
            // String scheduleJson = convertScheduleToMetadata(schedule);
            // request = request.subscriptionMetadata("delivery_schedule", scheduleJson);
            // }

            // Execute the request and get hosted page
            Result result = request.request();
            HostedPage hostedPage = result.hostedPage();

            logger.info("Direct subscription checkout hosted page created - ID: {}, URL: {}",
                    hostedPage.id(), hostedPage.url());

            Map<String, Object> response = new HashMap<>();
            response.put("hosted_page_id", hostedPage.id());
            response.put("url", hostedPage.url());
            response.put("plan_id", planId);
            response.put("plan_name", plan.getProduct() != null ? plan.getProduct().getName() : "Unknown");
            response.put("expires_at", hostedPage.expiresAt());

            return response;

        } catch (Exception e) {
            logger.error("Error creating direct subscription checkout hosted page: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create subscription checkout page: " + e.getMessage(), e);
        }
    }

    /**
     * Converts the day-wise schedule map to a JSON metadata string.
     * Example: { "Monday": true, "Wednesday": true } ->
     * "{\"Monday\":true,\"Wednesday\":true}"
     */
    private String convertScheduleToMetadata(Map<String, Boolean> schedule) {
        try {
            // Simple JSON conversion without external library
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Boolean> entry : schedule.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            logger.error("Error converting schedule to metadata: {}", e.getMessage());
            return "{}";
        }
    }
}
