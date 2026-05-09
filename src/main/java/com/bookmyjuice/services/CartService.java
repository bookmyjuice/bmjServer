package com.bookmyjuice.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.Cart;
import com.bookmyjuice.models.CartItem;
import com.bookmyjuice.models.OneTimePrice;
import com.bookmyjuice.models.SubscriptionPlan;
import com.bookmyjuice.models.User;
import com.bookmyjuice.repository.CartRepository;
import com.bookmyjuice.repository.OneTimePriceRepository;
import com.bookmyjuice.repository.SubscriptionPlanRepository;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepo;
    @Autowired
    private OneTimePriceRepository oneTimePriceRepo;
    @Autowired
    private SubscriptionPlanRepository planRepo;

    /**
     * Adds an item to the user's cart.
     * ENFORCEMENT: Cannot mix 'charge' and 'plan' items.
     */
    @Transactional
    public Map<String, Object> addItem(User user, String priceId, Integer quantity) {
        logger.info("Adding item to cart - User: {}, PriceId: {}, Quantity: {}", user.getId(), priceId, quantity);

        Cart cart = cartRepo.findByUserId(user.getId())
                .orElseGet(() -> {
                    logger.info("Creating new cart for user: {}", user.getId());
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepo.save(newCart);
                });

        // 1. Determine Item Type and Validate Unit Price
        Long unitPrice = 0L;
        String itemType = "";

        if (priceId.startsWith("charge_")) {
            itemType = "charge";
            OneTimePrice otp = oneTimePriceRepo.findById(priceId)
                    .orElseThrow(() -> {
                        logger.error("Invalid OneTimePrice ID: {}", priceId);
                        return new RuntimeException("Invalid Item ID: " + priceId);
                    });
            unitPrice = otp.getPrice();
        } else if (priceId.startsWith("plan_")) {
            itemType = "plan";
            SubscriptionPlan plan = planRepo.findById(priceId)
                    .orElseThrow(() -> {
                        logger.error("Invalid Plan ID: {}", priceId);
                        return new RuntimeException("Invalid Plan ID: " + priceId);
                    });
            unitPrice = plan.getPrice();
        } else {
            throw new IllegalArgumentException("Invalid Price ID format: " + priceId);
        }

        // 2. Enforce "No Mixed Carts" Rule
        if (!cart.getItems().isEmpty()) {
            String currentType = cart.getItems().get(0).getType();
            if (!currentType.equals(itemType)) {
                logger.warn("Cart mismatch error - User: {}, Current: {}, New: {}",
                        user.getId(), currentType, itemType);
                throw new RuntimeException(
                        "Cart mismatch error: You cannot mix One-Time items and Subscription plans. Please clear your cart first.");
            }
        }

        // 3. Add or Update Quantity
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getPriceId().equals(priceId)).findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(quantity);
            logger.info("Updated quantity for item: {} to {}", priceId, quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setPriceId(priceId);
            newItem.setQuantity(quantity);
            newItem.setType(itemType);
            cart.getItems().add(newItem);
            logger.info("Added new item to cart: {} with quantity {}", priceId, quantity);
        }

        Cart savedCart = cartRepo.save(cart);
        logger.info("Cart saved successfully - CartId: {}, Items: {}", savedCart.getId(), savedCart.getItems().size());

        return getCartResponse(savedCart);
    }

    /**
     * Returns the cart details with calculated totals.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCart(User user) {
        logger.info("Getting cart for user: {}", user.getId());

        Cart cart = cartRepo.findByUserId(user.getId())
                .orElseGet(() -> {
                    logger.info("No cart found for user: {}, creating empty cart", user.getId());
                    Cart emptyCart = new Cart();
                    emptyCart.setUser(user);
                    return cartRepo.save(emptyCart);
                });
        return getCartResponse(cart);
    }

    /**
     * Removes an item from the cart by priceId.
     */
    @Transactional
    public Map<String, Object> removeByPriceId(User user, String priceId) {
        logger.info("Removing item from cart - User: {}, PriceId: {}", user.getId(), priceId);

        Cart cart = cartRepo.findByUserId(user.getId())
                .orElseThrow(() -> {
                    logger.error("Cart not found for user: {}", user.getId());
                    return new RuntimeException("Cart not found");
                });

        boolean removed = cart.getItems().removeIf(item -> item.getPriceId().equals(priceId));

        if (!removed) {
            logger.warn("Item not found in cart - User: {}, PriceId: {}", user.getId(), priceId);
            throw new RuntimeException("Item not found in cart: " + priceId);
        }

        logger.info("Item removed successfully - User: {}, PriceId: {}", user.getId(), priceId);
        cartRepo.save(cart);
        return getCartResponse(cart);
    }

    /**
     * Clears all items from the user's cart.
     */
    @Transactional
    public Map<String, Object> clearCart(User user) {
        logger.info("Clearing cart for user: {}", user.getId());

        Cart cart = cartRepo.findByUserId(user.getId())
       Merges guest cart into authenticated user's cart with conflict resolution.
     * @param user The authenticated user
     * @param guestCartId The guest cart ID to merge
     * @param keepPreference "guest" or "user" when types conflict, null for auto-merge
     * @return Cart response map
     * @throws RuntimeException when cart types conflict and no preference given
     */

    @Transactional
    public Map<String, Object> mergeCarts(User user, String guestCartId, String keepPreference) {
        logger.info("Merging carts - User: {}, GuestCartId: {}, KeepPreference: {}",
                user.getId(), guestCartId, keepPreference);

        // Find both carts
        Cart userCart = cartRepo.findByUserId(user.getId()).orElse(null);
        Cart guestCart = cartRepo.findById(guestCartId).orElse(null);

        if (guestCart == null) {
            throw new IllegalArgumentException("Guest cart not found: " + guestCartId);
        }

        if (guestCart.getUser() != null) {
            throw new IllegalArgumentException("Cart is not a guest cart: " + guestCartId);
        }

        // If user has no cart, simply assign guest cart to user
        if (userCart == null) {
            guestCart.setUser(user);
            cartRepo.save(guestCart);
            logger.info("Assigned guest cart to user: {}", user.getId());
            return getCartResponse(guestCart);
        }

        // Both carts exist - check for type conflicts
        String userCartType = getCartType(userCart);
        String guestCartType = getCartType(guestCart);

        // If types are different and no preference given, return conflict info
        if (!userCartType.equals(guestCartType) && keepPreference == null) {
            logger.warn("Cart type conflict - User: {}, UserType: {}, GuestType: {}",
                    user.getId(), userCartType, guestCartType);
            throw new RuntimeException("Cart type conflict: user_cart_type=" + userCartType +
                    ", guest_cart_type=" + guestCartType);
        }

        // Handle conflict resolution
        if (!userCartType.equals(guestCartType)) {
            if ("guest".equals(keepPreference)) {
                // Keep guest cart, delete user cart
                cartRepo.delete(userCart);
                guestCart.setUser(user);
                cartRepo.save(guestCart);
                logger.info("Kept guest cart, deleted user cart for user: {}", user.getId());
                return getCartResponse(guestCart);
            } else if ("user".equals(keepPreference)) {
                // Keep user cart, delete guest cart
                cartRepo.delete(guestCart);
                logger.info("Kept user cart, deleted guest cart for user: {}", user.getId());
                return getCartResponse(userCart);
            } else {
                throw new IllegalArgumentException("Invalid keep preference: " + keepPreference);
            }
        }

        // Same cart types - merge items
        mergeCartItems(userCart, guestCart);
        cartRepo.delete(guestCart); // Guest cart no longer needed
        Cart savedCart = cartRepo.save(userCart);

        logger.info("Successfully merged carts for user: {}", user.getId());
        return getCartResponse(savedCart);
    }

    /**
     * Determines the cart type based on items.
     */
    private String getCartType(Cart cart) {
        if (cart.getItems().isEmpty()) {
            return "empty";
        }
        return cart.getItems().get(0).getType(); // All items have same type due to enforcement
    }

    /**
     * Merges items from guest cart into user cart.
     * Combines quantities for duplicate items.
     */
    private void mergeCartItems(Cart userCart, Cart guestCart) {
        for (CartItem guestItem : guestCart.getItems()) {
            Optional<CartItem> existingItem = userCart.getItems().stream()
                    .filter(item -> item.getPriceId().equals(guestItem.getPriceId()))
                    .findFirst();

            if (existingItem.isPresent()) {
                // Combine quantities
                existingItem.get().setQuantity(
                        existingItem.get().getQuantity() + guestItem.getQuantity());
                logger.debug("Combined quantities for item: {}", guestItem.getPriceId());
            } else {
                // Add new item
                CartItem newItem = new CartItem();
                newItem.setCart(userCart);
                newItem.setPriceId(guestItem.getPriceId());
                newItem.setQuantity(guestItem.getQuantity());
                newItem.setType(guestItem.getType());
                userCart.getItems().add(newItem);
                logger.debug("Added new item from guest cart: {}", guestItem.getPriceId());
            }
        }
    }

    /**
     * .orElseThrow(() -> {
     * logger.error("Cart not found for user: {}", user.getId());
     * return new RuntimeException("Cart not found");
     * });
     * 
     * cart.getItems().clear();
     * cartRepo.save(cart);
     * 
     * logger.info("Cart cleared for user: {}", user.getId());
     * return getCartResponse(cart);
     * }
     * 
     * /**
     * Merges guest cart into authenticated user's cart with conflict resolution.
     * 
     * @param user           The authenticated user
     * @param guestCartId    The guest cart ID to merge
     * @param keepPreference "guest" or "user" when types conflict, null for
     *                       auto-merge
     * @return Cart response map
     * @throws RuntimeException when cart types conflict and no preference given
     */
    @Transactional
    public Map<String, Object> mergeCarts(User user, String guestCartId, String keepPreference) {
        logger.info("Merging carts - User: {}, GuestCartId: {}, KeepPreference: {}",
                user.getId(), guestCartId, keepPreference);

        // Find both carts
        Cart userCart = cartRepo.findByUserId(user.getId()).orElse(null);
        Cart guestCart = cartRepo.findById(guestCartId).orElse(null);

        if (guestCart == null) {
            throw new IllegalArgumentException("Guest cart not found: " + guestCartId);
        }

        if (guestCart.getUser() != null) {
            throw new IllegalArgumentException("Cart is not a guest cart: " + guestCartId);
        }

        // If user has no cart, simply assign guest cart to user
        if (userCart == null) {
            guestCart.setUser(user);
            cartRepo.save(guestCart);
            logger.info("Assigned guest cart to user: {}", user.getId());
            return getCartResponse(guestCart);
        }

        // Both carts exist - check for type conflicts
        String userCartType = getCartType(userCart);
        String guestCartType = getCartType(guestCart);

        // If types are different and no preference given, return conflict info
        if (!userCartType.equals(guestCartType) && keepPreference == null) {
            logger.warn("Cart type conflict - User: {}, UserType: {}, GuestType: {}",
                    user.getId(), userCartType, guestCartType);
            throw new RuntimeException("Cart type conflict: user_cart_type=" + userCartType +
                    ", guest_cart_type=" + guestCartType);
        }

        // Handle conflict resolution
        if (!userCartType.equals(guestCartType)) {
            if ("guest".equals(keepPreference)) {
                // Keep guest cart, delete user cart
                cartRepo.delete(userCart);
                guestCart.setUser(user);
                cartRepo.save(guestCart);
                logger.info("Kept guest cart, deleted user cart for user: {}", user.getId());
                return getCartResponse(guestCart);
            } else if ("user".equals(keepPreference)) {
                // Keep user cart, delete guest cart
                cartRepo.delete(guestCart);
                logger.info("Kept user cart, deleted guest cart for user: {}", user.getId());
                return getCartResponse(userCart);
            } else {
                throw new IllegalArgumentException("Invalid keep preference: " + keepPreference);
            }
        }

        // Same cart types - merge items
        mergeCartItems(userCart, guestCart);
        cartRepo.delete(guestCart); // Guest cart no longer needed
        Cart savedCart = cartRepo.save(userCart);

        logger.info("Successfully merged carts for user: {}", user.getId());
        return getCartResponse(savedCart);
    }

    /**
     * Determines the cart type based on items.
     */
    private String getCartType(Cart cart) {
        if (cart.getItems().isEmpty()) {
            return "empty";
        }
        return cart.getItems().get(0).getType(); // All items have same type due to enforcement
    }

    /**
     * Merges items from guest cart into user cart.
     * Combines quantities for duplicate items.
     */
    private void mergeCartItems(Cart userCart, Cart guestCart) {
        for (CartItem guestItem : guestCart.getItems()) {
            Optional<CartItem> existingItem = userCart.getItems().stream()
                    .filter(item -> item.getPriceId().equals(guestItem.getPriceId()))
                    .findFirst();

            if (existingItem.isPresent()) {
                // Combine quantities
                existingItem.get().setQuantity(
                        existingItem.get().getQuantity() + guestItem.getQuantity());
                logger.debug("Combined quantities for item: {}", guestItem.getPriceId());
            } else {
                // Add new item
                CartItem newItem = new CartItem();
                newItem.setCart(userCart);
                newItem.setPriceId(guestItem.getPriceId());
                newItem.setQuantity(guestItem.getQuantity());
                newItem.setType(guestItem.getType());
                userCart.getItems().add(newItem);
                logger.debug("Added new item from guest cart: {}", guestItem.getPriceId());
            }
        }
    }

    /**
     * Helper to calculate totals securely on the backend.
     */
    private Map<String, Object> getCartResponse(Cart cart) {
        long subtotal = 0;

        for (CartItem item : cart.getItems()) {
            // Fetch price from DB (Security: don't trust client side prices)
            if ("charge".equals(item.getType())) {
                OneTimePrice p = oneTimePriceRepo.findById(item.getPriceId()).orElse(null);
                if (p != null)
                    subtotal += p.getPrice() * item.getQuantity();
            } else if ("plan".equals(item.getType())) {
                SubscriptionPlan p = planRepo.findById(item.getPriceId()).orElse(null);
                if (p != null)
                    subtotal += p.getPrice() * item.getQuantity();
            }
        }

        // Business Rule: Delivery fee logic (e.g., Free above ₹500 / 50000 paise)
        long deliveryFee = (subtotal >= 50000) ? 0 : 4000;
        long tax = 0; // Add tax logic if needed
        long grandTotal = subtotal + deliveryFee + tax;

        Map<String, Object> response = new HashMap<>();
        response.put("cart_id", cart.getId());
        response.put("items", cart.getItems());
        response.put("subtotal", subtotal);
        response.put("delivery_fee", deliveryFee);
        response.put("tax", tax);
        response.put("grand_total", grandTotal);

        return response;
    }
}
