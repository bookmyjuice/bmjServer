package com.bookmyjuice.services;

import com.bookmyjuice.models.*;
import com.bookmyjuice.repository.*;
import com.chargebee.Result;
import com.chargebee.models.HostedPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for handling checkout operations via Chargebee Hosted Pages.
 * Supports one-time checkout for cart items.
 */
@Service
public class CheckoutService {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private OneTimePriceRepository oneTimePriceRepo;

    @Autowired
    private UserRepository userRepository;

    /**
     * Creates a Chargebee Hosted Page URL for one-time checkout.
     * The user's cart must contain only 'charge' type items (no mixed carts).
     * 
     * @param user The authenticated user
     * @return Map containing hosted page URL and ID
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkoutOneTime(User user) {
        logger.info("Initiating one-time checkout for user: {}", user.getId());

        // Get user's cart
        Cart cart = cartRepo.findByUserId(user.getId())
                .orElseThrow(() -> {
                    logger.error("Cart not found for user: {}", user.getId());
                    return new RuntimeException("Cart not found. Please add items to cart first.");
                });

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Please add items before checkout.");
        }

        // Verify cart contains only 'charge' type items
        for (CartItem item : cart.getItems()) {
            if (!"charge".equals(item.getType())) {
                throw new RuntimeException("Cart contains non-one-time items. Please clear cart and add only one-time items.");
            }
        }

        // Get chargebee customer ID from user
        String chargebeeCustomerId = user.getChargebeeCustomerId();
        if (chargebeeCustomerId == null || chargebeeCustomerId.isEmpty()) {
            throw new RuntimeException("Chargebee customer ID not found. Please ensure your profile is set up.");
        }

        try {
            // Build the checkout request with cart items
            HostedPage.CheckoutOneTimeForItemsRequest request = HostedPage.checkoutOneTimeForItems()
                    .customerId(chargebeeCustomerId);

            // Add all cart items to the checkout request
            int index = 0;
            for (CartItem item : cart.getItems()) {
                request = request.itemPriceItemPriceId(index, item.getPriceId())
                               .itemPriceQuantity(index, item.getQuantity());
                index++;
            }

            // Execute the request and get hosted page
            Result result = request.request();
            HostedPage hostedPage = result.hostedPage();

            logger.info("One-time checkout hosted page created - ID: {}, URL: {}", 
                hostedPage.id(), hostedPage.url());

            Map<String, Object> response = new HashMap<>();
            response.put("hosted_page_id", hostedPage.id());
            response.put("url", hostedPage.url());
            response.put("cart_id", cart.getId());
            response.put("expires_at", hostedPage.expiresAt());
            
            return response;

        } catch (Exception e) {
            logger.error("Error creating one-time checkout hosted page: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create checkout page: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a Chargebee Hosted Page URL for one-time checkout with specific items.
     * Used when bypassing the cart system.
     * 
     * @param user The authenticated user
     * @param priceItems List of price item IDs with quantities
     * @return Map containing hosted page URL and ID
     */
    @Transactional(readOnly = true)
    public Map<String, Object> checkoutOneTimeWithItems(User user, List<Map<String, Object>> priceItems) {
        logger.info("Initiating one-time checkout with specific items for user: {}", user.getId());

        if (priceItems == null || priceItems.isEmpty()) {
            throw new RuntimeException("No items provided for checkout.");
        }

        // Get chargebee customer ID from user
        String chargebeeCustomerId = user.getChargebeeCustomerId();
        if (chargebeeCustomerId == null || chargebeeCustomerId.isEmpty()) {
            throw new RuntimeException("Chargebee customer ID not found. Please ensure your profile is set up.");
        }

        try {
            // Build the checkout request with provided items
            HostedPage.CheckoutOneTimeForItemsRequest request = HostedPage.checkoutOneTimeForItems()
                    .customerId(chargebeeCustomerId);

            int index = 0;
            for (Map<String, Object> item : priceItems) {
                String priceId = (String) item.get("price_id");
                Integer quantity = item.get("quantity") != null ? (Integer) item.get("quantity") : 1;
                
                request = request.itemPriceItemPriceId(index, priceId)
                                   .itemPriceQuantity(index, quantity);
                index++;
            }

            // Execute the request and get hosted page
            Result result = request.request();
            HostedPage hostedPage = result.hostedPage();

            logger.info("One-time checkout hosted page created - ID: {}, URL: {}", 
                hostedPage.id(), hostedPage.url());

            Map<String, Object> response = new HashMap<>();
            response.put("hosted_page_id", hostedPage.id());
            response.put("url", hostedPage.url());
            response.put("expires_at", hostedPage.expiresAt());
            
            return response;

        } catch (Exception e) {
            logger.error("Error creating one-time checkout hosted page: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create checkout page: " + e.getMessage(), e);
        }
    }
}
