package com.bookmyjuice.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.repository.ItemPriceRepository;
import com.bookmyjuice.repository.PlanRepository;
import com.bookmyjuice.models.entities.ItemPriceEntity;
import com.chargebee.Result;
import com.chargebee.models.HostedPage;
import com.chargebee.models.Item;
import com.chargebee.models.ItemPrice;
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

    @Autowired
    private ItemPriceRepository itemPriceRepository;

    @Autowired
    private DeliveryScheduleService deliveryScheduleService;


    /**
     * Create a subscription hosted page for the customer to purchase a plan
     */
    public HostedPage createSubscriptionHostedPage(String customerId, String planId) throws Exception {
        return createSubscriptionHostedPage(customerId, planId, null);
    }

    /**
     * Same as {@link #createSubscriptionHostedPage(String, String)} but forces
     * the Chargebee subscription id.
     *
     * <p>
     * The id is reserved by the checkout service so the day-wise delivery
     * schedule can be stored before the customer pays. Chargebee's
     * {@code checkout_new_for_items} accepts {@code subscription[id]} but not
     * {@code subscription[meta_data]}, so the reserved id is the join key
     * between the stored schedule and the subscription_created webhook.
     * </p>
     */
    public HostedPage createSubscriptionHostedPage(String customerId, String planId, String reservedSubscriptionId)
            throws Exception {
        logger.info("Creating subscription hosted page for customer: {} to plan: {}", customerId, planId);

        try {
            HostedPage.CheckoutNewForItemsRequest request = buildHostedPageRequest(customerId, planId,
                    reservedSubscriptionId);

            Result result = request.request();

            HostedPage hostedPage = result.hostedPage();
            logger.info("Subscription hosted page created successfully: {}", hostedPage.id());

            return hostedPage;
        } catch (Exception e) {
            logger.error("Error creating subscription hosted page: {}", e.getMessage(), e);
            throw new Exception("Failed to create subscription hosted page: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the Product Catalog 2.0 hosted-page request for a new subscription.
     *
     * <p>
     * The catalog 1.0 call ({@code HostedPage.checkoutNew()} with
     * {@code subscription[plan_id]}) is rejected on this site with "API endpoint
     * is incompatible with the product catalog version", so the plan is passed
     * the 2.0 way: as the {@code item_price_id} of the subscription's single
     * item. Everything else is unchanged — the pre-reserved subscription id
     * still travels as {@code subscription[id]}, which is what lets the
     * delivery schedule stored before payment be joined to the
     * {@code subscription_created} webhook.
     * </p>
     *
     * @param customerId             Chargebee customer the subscription belongs to
     * @param planId                 item price id of the plan, e.g.
     *                               {@code bmj-delight-mix-punch-200ml-weekly}
     * @param reservedSubscriptionId pre-reserved Chargebee subscription id, may be null
     * @return the un-sent request, so callers (and tests) can inspect its params
     */
    HostedPage.CheckoutNewForItemsRequest buildHostedPageRequest(String customerId, String planId,
            String reservedSubscriptionId) {
        HostedPage.CheckoutNewForItemsRequest request = HostedPage.checkoutNewForItems()
                .customerId(customerId)
                .subscriptionItemItemPriceId(0, planId);

        if (reservedSubscriptionId != null && !reservedSubscriptionId.isEmpty()) {
            request.subscriptionId(reservedSubscriptionId);
        }

        return request;
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
     * List all subscription plans from the local database.
     * The database is synced from Chargebee (Product Catalog 2.0) at startup
     * via ChargebeeSyncService, which syncs Items and ItemPrices.
     * 
     * Falls back to reading from plan_entity table (legacy schema) if
     * item prices are not available. Returns an empty list gracefully
     * if no plan data is available in either source.
     */
    public List<Map<String, Object>> getAllPlans() throws Exception {
        logger.info("Fetching all subscription plans from local database");
        List<Map<String, Object>> plans = new ArrayList<>();

        try {
            // First try: Fetch from synced ItemPrice/Item data (Catalog 2.0)
            List<ItemPriceEntity> itemPrices = itemPriceRepository.findAll();
            
            if (!itemPrices.isEmpty()) {
                for (ItemPriceEntity ip : itemPrices) {
                    if (ip.getItem() == null) continue;
                    
                    Map<String, Object> planMap = new HashMap<>();
                    planMap.put("id", ip.getId());
                    planMap.put("name", ip.getItem().getName() != null ? ip.getItem().getName() : ip.getName());
                    planMap.put("description", ip.getDescription());
                    planMap.put("price", ip.getPrice());
                    planMap.put("currencyCode", ip.getCurrencyCode());
                    planMap.put("period", ip.getPeriod());
                    planMap.put("periodUnit", ip.getPeriodUnit());
                    planMap.put("status", ip.getStatus());

                    // Add structured fields derived from item name/id
                    String itemName = ip.getItem().getName() != null ? ip.getItem().getName() : "";
                    planMap.put("category", extractCategory(itemName));
                    planMap.put("sizeLabel", extractSize(itemName));
                    planMap.put("period", extractPeriodLabel(itemName, ip.getPeriodUnit()));

                    plans.add(planMap);
                }
                logger.info("Successfully fetched {} plans from local DB (ItemPrices)", plans.size());
                return plans;
            }
            
            // Second try: Fetch from local plan_entity table
            List<com.bookmyjuice.models.entities.PlanEntity> dbPlans = planRepository.findAll();
            if (!dbPlans.isEmpty()) {
                for (com.bookmyjuice.models.entities.PlanEntity plan : dbPlans) {
                    Map<String, Object> planMap = new HashMap<>();
                    planMap.put("id", plan.getId());
                    planMap.put("name", plan.getName());
                    planMap.put("description", plan.getDescription());
                    planMap.put("price", plan.getPrice());
                    planMap.put("period", plan.getPeriod());
                    planMap.put("periodUnit", plan.getPeriodUnit());
                    planMap.put("status", plan.getStatus());
                    plans.add(planMap);
                }
                logger.info("Successfully fetched {} plans from local plan_entity table", plans.size());
                return plans;
            }
            
            logger.warn("No plans found in local database, falling back to Chargebee API");
            plans = fetchPlansFromChargebeeApi();
            if (!plans.isEmpty()) {
                logger.info("Fetched {} plans from Chargebee API fallback", plans.size());
                return plans;
            }
            
        } catch (Exception e) {
            logger.error("Error fetching plans from local DB: {}", e.getMessage(), e);
            // Try Chargebee API as fallback
            try {
                plans = fetchPlansFromChargebeeApi();
                if (!plans.isEmpty()) {
                    logger.info("Fetched {} plans from Chargebee API fallback after DB error", plans.size());
                    return plans;
                }
            } catch (Exception apiEx) {
                logger.error("Chargebee API fallback also failed: {}", apiEx.getMessage());
            }
            logger.warn("Returning empty plans list due to error: {}", e.getMessage());
        }

        return plans;
    }

    /*
     * Retired: the plan-less pricing page is a catalog 1.0 concept.
     *
     * <p>
     * {@code HostedPage.checkoutNew()} without items only exists for catalog
     * 1.0; its catalog 2.0 replacement, {@code checkout_new_for_items}, requires
     * at least one {@code item_price_id}, so there is nothing to migrate this
     * call to. The product surface no longer uses a hosted pricing page at all:
     * plan browsing comes from {@link #getAllPlans()} and the app's native
     * plan-selection screen, and the old route
     * ({@code PricingPageController}) answers {@code 410 Gone}. The method was
     * therefore removed rather than left as a call that can only fail with
     * "API endpoint is incompatible with the product catalog version".
     * </p>
     */

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
     * Fetch plans directly from Chargebee API when local DB is empty.
     * Uses ItemPrice.list() to get all item prices with their item data.
     */
    private List<Map<String, Object>> fetchPlansFromChargebeeApi() {
        List<Map<String, Object>> plans = new ArrayList<>();
        try {
            logger.info("Fetching subscription plans directly from Chargebee API");
            String offset = null;

            do {
                com.chargebee.ListResult priceResult;
                if (offset == null) {
                    priceResult = ItemPrice.list().limit(100).request();
                } else {
                    priceResult = ItemPrice.list().limit(100).offset(offset).request();
                }

                for (com.chargebee.ListResult.Entry entry : priceResult) {
                    ItemPrice ip = entry.itemPrice();

                    Map<String, Object> planMap = new HashMap<>();
                    planMap.put("id", ip.id());
                    planMap.put("name", ip.name());
                    planMap.put("description", ip.description());
                    planMap.put("price", ip.price() != null ? BigDecimal.valueOf(ip.price()).movePointLeft(2) : null);
                    planMap.put("currencyCode", ip.currencyCode());
                    planMap.put("period", ip.period());
                    planMap.put("periodUnit", ip.periodUnit() != null ? ip.periodUnit().name() : null);
                    planMap.put("status", ip.status() != null ? ip.status().name() : "active");

                    // Add structured fields
                    String itemName = ip.name() != null ? ip.name() : "";
                    planMap.put("category", extractCategory(itemName));
                    planMap.put("sizeLabel", extractSize(itemName));
                    planMap.put("period", extractPeriodLabel(itemName,
                            ip.periodUnit() != null ? ip.periodUnit().name() : null));

                    plans.add(planMap);
                }

                offset = priceResult.nextOffset();
            } while (offset != null);

            logger.info("Fetched {} plans from Chargebee API (all pages)", plans.size());
        } catch (Exception e) {
            logger.error("Failed to fetch plans from Chargebee API: {}", e.getMessage());
        }
        return plans;
    }

    /**
     * Extract category from item name (e.g. "Delight 200ml Weekly" → "delight").
     */
    private String extractCategory(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("delight")) return "delight";
        if (lower.contains("signature")) return "signature";
        if (lower.contains("premium")) return "premium";
        return "delight";
    }

    /**
     * Extract size label from item name (e.g. "Delight 200ml Weekly" → "200").
     */
    private String extractSize(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("200ml") || lower.contains("200 ml")) return "200";
        if (lower.contains("300ml") || lower.contains("300 ml")) return "300";
        if (lower.contains("500ml") || lower.contains("500 ml")) return "500";
        return "200";
    }

    /**
     * Derive a human-readable period label from item name or period unit.
     */
    private String extractPeriodLabel(String name, String periodUnit) {
        String lower = name.toLowerCase();
        if (lower.contains("weekly") || lower.contains("week")) return "Weekly";
        if (lower.contains("monthly") || lower.contains("month")) return "Monthly";

        if (periodUnit != null) {
            String unit = periodUnit.toLowerCase();
            if (unit.equals("week")) return "Weekly";
            if (unit.equals("month")) return "Monthly";
        }
        return "Weekly";
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
        // Delivery date is only published from a schedule the customer actually
        // submitted at checkout; nothing is inferred from the billing cycle.
        Integer startedAt = subscription.startedAt() != null
                ? (int) (subscription.startedAt().getTime() / 1000L)
                : 0;
        map.put("startedAt", startedAt);
        map.put("nextDeliveryAt", deliveryScheduleService.nextDeliveryAt(
                subscription.id(), startedAt,
                subscription.status() != null ? subscription.status().toString() : null));
        map.put("deliverySchedule", deliveryScheduleService.scheduleFor(subscription.id()));
        return map;
    }
}
