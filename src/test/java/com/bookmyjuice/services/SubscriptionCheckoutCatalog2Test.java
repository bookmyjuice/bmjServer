package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.chargebee.internal.HttpUtil;
import com.chargebee.models.HostedPage;

/**
 * Pins the Product Catalog 2.0 migration of subscription checkout.
 *
 * <p>
 * This site is on catalog 2.0, where {@code HostedPage.checkoutNew()} (catalog
 * 1.0, {@code subscription[plan_id]}) fails with "API endpoint is incompatible
 * with the product catalog version". Every subscription hosted page must
 * therefore be built by {@code checkout_new_for_items} with the plan carried as
 * {@code subscription_items[item_price_id][0]}.
 * </p>
 *
 * <p>
 * No network is involved: the builders under test return the un-sent request,
 * and the assertions read the exact params Chargebee would receive.
 * </p>
 */
class SubscriptionCheckoutCatalog2Test {

    private static final String CUSTOMER_ID = "bmj-cust-0001";
    /** Real, active catalog 2.0 plan item prices (verified on the test site). */
    private static final String WEEKLY_PLAN = "bmj-delight-mix-punch-200ml-weekly";
    private static final String MONTHLY_PLAN = "bmj-delight-mix-punch-200ml-monthly";
    private static final String RESERVED_SUBSCRIPTION_ID = "bmj-sub-reserved-0001";

    private static final String CATALOG_2_URI = "/hosted_pages/checkout_new_for_items";
    /** Catalog 1.0 only; must never appear on a request built here. */
    private static final String LEGACY_PLAN_PARAM = "subscription[plan_id]";

    private static Map<String, Object> paramsOf(HostedPage.CheckoutNewForItemsRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : request.params().entries()) {
            params.put(entry.getKey(), entry.getValue());
        }
        return params;
    }

    @Nested
    @DisplayName("SubscriptionApiService (POST /api/subscriptions/create)")
    class ApiService {

        private final SubscriptionApiService service = new SubscriptionApiService();

        @Test
        @DisplayName("weekly plan is sent as a catalog 2.0 item price")
        void buildsCatalog2RequestForWeeklyPlan() {
            HostedPage.CheckoutNewForItemsRequest request = service
                    .buildHostedPageRequest(CUSTOMER_ID, WEEKLY_PLAN, RESERVED_SUBSCRIPTION_ID);

            assertEquals(HttpUtil.Method.POST, request.httpMeth());
            assertEquals(CATALOG_2_URI, request.uri());

            Map<String, Object> params = paramsOf(request);
            assertEquals(WEEKLY_PLAN, params.get("subscription_items[item_price_id][0]"),
                    "the plan must be the item price of the first subscription item");
            assertEquals(CUSTOMER_ID, params.get("customer[id]"));
            assertEquals(RESERVED_SUBSCRIPTION_ID, params.get("subscription[id]"),
                    "the reserved id is the join key to the stored delivery schedule");
            assertFalse(params.containsKey(LEGACY_PLAN_PARAM),
                    "catalog 1.0 subscription[plan_id] must not be sent");
        }

        @Test
        @DisplayName("monthly plan is sent as a catalog 2.0 item price")
        void buildsCatalog2RequestForMonthlyPlan() {
            Map<String, Object> params = paramsOf(
                    service.buildHostedPageRequest(CUSTOMER_ID, MONTHLY_PLAN, null));

            assertEquals(MONTHLY_PLAN, params.get("subscription_items[item_price_id][0]"));
            assertEquals(CUSTOMER_ID, params.get("customer[id]"));
        }

        @Test
        @DisplayName("no subscription[id] is sent when there is no reserved id")
        void omitsReservationWhenAbsent() {
            assertFalse(paramsOf(service.buildHostedPageRequest(CUSTOMER_ID, WEEKLY_PLAN, null))
                    .containsKey("subscription[id]"));
            assertFalse(paramsOf(service.buildHostedPageRequest(CUSTOMER_ID, WEEKLY_PLAN, ""))
                    .containsKey("subscription[id]"));
        }

        @Test
        @DisplayName("a different plan id does not leak the previous one")
        void itemPriceFollowsTheArgument() {
            Object weekly = paramsOf(service.buildHostedPageRequest(CUSTOMER_ID, WEEKLY_PLAN, null))
                    .get("subscription_items[item_price_id][0]");
            Object monthly = paramsOf(service.buildHostedPageRequest(CUSTOMER_ID, MONTHLY_PLAN, null))
                    .get("subscription_items[item_price_id][0]");

            assertNotEquals(weekly, monthly);
            assertEquals(WEEKLY_PLAN, weekly);
            assertEquals(MONTHLY_PLAN, monthly);
        }
    }

    @Nested
    @DisplayName("SubscriptionCheckoutService (cart + direct)")
    class CheckoutService {

        private final SubscriptionCheckoutService service = new SubscriptionCheckoutService();

        @Test
        @DisplayName("cart checkout sends the plan as a catalog 2.0 item price")
        void buildsCatalog2RequestForCartFlow() {
            HostedPage.CheckoutNewForItemsRequest request = service
                    .buildHostedPageRequest(CUSTOMER_ID, WEEKLY_PLAN);

            assertEquals(HttpUtil.Method.POST, request.httpMeth());
            assertEquals(CATALOG_2_URI, request.uri());

            Map<String, Object> params = paramsOf(request);
            assertEquals(WEEKLY_PLAN, params.get("subscription_items[item_price_id][0]"));
            assertEquals(CUSTOMER_ID, params.get("customer[id]"));
            assertFalse(params.containsKey(LEGACY_PLAN_PARAM));
        }

        @Test
        @DisplayName("direct checkout sends the plan as a catalog 2.0 item price")
        void buildsCatalog2RequestForDirectFlow() {
            Map<String, Object> params = paramsOf(
                    service.buildHostedPageRequest(CUSTOMER_ID, MONTHLY_PLAN));

            assertEquals(MONTHLY_PLAN, params.get("subscription_items[item_price_id][0]"));
            assertEquals(CUSTOMER_ID, params.get("customer[id]"));
            assertFalse(params.containsKey(LEGACY_PLAN_PARAM));
        }

        @Test
        @DisplayName("quantity is not sent, so the charged amount is unchanged by the migration")
        void doesNotSendQuantity() {
            Map<String, Object> params = paramsOf(service.buildHostedPageRequest(CUSTOMER_ID, WEEKLY_PLAN));

            assertFalse(params.containsKey("subscription_items[quantity][0]"),
                    "plan quantities stay unsent exactly as they were before the migration");
            assertEquals(2, params.size(),
                    "only customer[id] and subscription_items[item_price_id][0] are sent");
            assertTrue(params.containsKey("subscription_items[item_price_id][0]"));
        }
    }
}
