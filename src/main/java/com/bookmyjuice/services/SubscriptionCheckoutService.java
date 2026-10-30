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

    @Autowired
    private DeliveryScheduleService deliveryScheduleService;

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
            // Build the checkout request.
            // Catalog 2.0: the plan is carried as the item price of the
            // subscription's single item. The catalog 1.0 form
            // (subscription[plan_id]) is rejected by this site with
            // "API endpoint is incompatible with the product catalog version".
            HostedPage.CheckoutNewForItemsRequest request = buildHostedPageRequest(chargebeeCustomerId, planId);

            // Quantity > 1 on a plan item is not part of the V1 contract and was
            // never sent before this migration either — the cart row's quantity
            // is deliberately still ignored, so the amount the customer is
            // charged does not change as a side effect of switching endpoints.
            // Catalog 2.0 can express it (subscriptionItemQuantity(0, quantity))
            // if the product ever decides plans are multi-quantity.
            logger.debug("Subscription quantity: {}", quantity);

            // Persist the day-wise schedule under a subscription id reserved
            // here, and hand that id to Chargebee. checkout_new_for_items
            // accepts subscription[id] but not subscription[meta_data], so
            // reserving the id is what lets the subscription_created webhook
            // join the schedule back to the subscription without guessing.
            String reservedSubscriptionId = deliveryScheduleService.reserve(chargebeeCustomerId, schedule);
            if (reservedSubscriptionId != null) {
                request.subscriptionId(reservedSubscriptionId);
                logger.info("Reserved subscription id {} for the delivery schedule", reservedSubscriptionId);
            }

            // Execute the request and get hosted page
            Result result = request.request();
            HostedPage hostedPage = result.hostedPage();

            logger.info("Subscription checkout hosted page created - ID: {}, URL: {}",
                    hostedPage.id(), hostedPage.url());

            Map<String, Object> response = new HashMap<>();
            response.put("hosted_page_id", hostedPage.id());
            response.put("url", hostedPage.url());
            response.put("cart_id", cart.getId());
            if (reservedSubscriptionId != null) {
                response.put("subscription_id", reservedSubscriptionId);
            }
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
            // Build the checkout request.
            // Catalog 2.0: the plan is an item price, not subscription[plan_id]
            // (catalog 1.0), which this site rejects outright.
            HostedPage.CheckoutNewForItemsRequest request = buildHostedPageRequest(chargebeeCustomerId, planId);

            // Same reservation as the cart flow: the schedule is persisted first
            // and the reserved id is passed to Chargebee so the webhook can join
            // the two records deterministically.
            String reservedSubscriptionId = deliveryScheduleService.reserve(chargebeeCustomerId, schedule);
            if (reservedSubscriptionId != null) {
                request.subscriptionId(reservedSubscriptionId);
                logger.info("Reserved subscription id {} for the direct delivery schedule", reservedSubscriptionId);
            }

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
            if (reservedSubscriptionId != null) {
                response.put("subscription_id", reservedSubscriptionId);
            }

            return response;

        } catch (Exception e) {
            logger.error("Error creating direct subscription checkout hosted page: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create subscription checkout page: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the Product Catalog 2.0 hosted-page request for a new subscription.
     *
     * <p>
     * Both subscription flows (cart and direct) go through here so the plan is
     * always sent the catalog 2.0 way — as the {@code item_price_id} of the
     * subscription's single item — instead of the retired catalog 1.0
     * {@code subscription[plan_id]}. The reserved subscription id is added by
     * the caller through {@code subscription[id]}.
     * </p>
     *
     * @param chargebeeCustomerId Chargebee customer the subscription belongs to
     * @param planId              item price id of the plan, e.g.
     *                            {@code bmj-delight-mix-punch-200ml-monthly}
     * @return the un-sent request, so callers (and tests) can inspect its params
     */
    HostedPage.CheckoutNewForItemsRequest buildHostedPageRequest(String chargebeeCustomerId, String planId) {
        return HostedPage.checkoutNewForItems()
                .customerId(chargebeeCustomerId)
                .subscriptionItemItemPriceId(0, planId);
    }
}
