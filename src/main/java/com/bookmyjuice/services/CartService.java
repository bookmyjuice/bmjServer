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
import com.bookmyjuice.repository.CartItemRepository;
import com.bookmyjuice.repository.CartRepository;
import com.bookmyjuice.repository.OneTimePriceRepository;
import com.bookmyjuice.repository.SubscriptionPlanRepository;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepo;

    @Autowired
    private CartItemRepository cartItemRepo;

    @Autowired
    private OneTimePriceRepository oneTimePriceRepo;

    @Autowired
    private SubscriptionPlanRepository planRepo;

    // ──────────────────────────────────────────────
    // Add Item
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, Object> addItem(User user, String priceId, Integer quantity) {
        logger.info("Adding item to cart - User: {}, PriceId: {}, Quantity: {}",
                user.getId(), priceId, quantity);

        Cart cart = cartRepo.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepo.save(newCart);
                });

        // 1. Determine item type and validate
        String itemType;
        if (priceId.startsWith("charge_")) {
            itemType = "charge";
            OneTimePrice otp = oneTimePriceRepo.findById(priceId)
                    .orElseThrow(() -> new RuntimeException("Invalid Item ID: " + priceId));
            // unitPrice = otp.getPrice();  // validated in getCartResponse
        } else if (priceId.startsWith("plan_")) {
            itemType = "plan";
            SubscriptionPlan plan = planRepo.findById(priceId)
                    .orElseThrow(() -> new RuntimeException("Invalid Plan ID: " + priceId));
            // unitPrice = plan.getPrice();
        } else {
            throw new IllegalArgumentException("Invalid Price ID format: " + priceId);
        }

        // 2. Enforce no mixed carts
        if (!cart.getItems().isEmpty()) {
            String currentType = cart.getItems().get(0).getType();
            if (!currentType.equals(itemType)) {
                throw new RuntimeException(
                        "Cart mismatch error: Cannot mix One-Time items and Subscription plans. Clear cart first.");
            }
        }

        // 3. Add or update quantity
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getPriceId().equals(priceId))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setPriceId(priceId);
            newItem.setQuantity(quantity);
            newItem.setType(itemType);
            cart.getItems().add(newItem);
        }

        Cart saved = cartRepo.save(cart);
        return getCartResponse(saved);
    }

    // ──────────────────────────────────────────────
    // Get Cart
    // ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getCart(User user) {
        logger.info("Getting cart for user: {}", user.getId());
        Cart cart = cartRepo.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart empty = new Cart();
                    empty.setUser(user);
                    return cartRepo.save(empty);
                });
        return getCartResponse(cart);
    }

    // ──────────────────────────────────────────────
    // Remove Item by Price ID
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, Object> removeByPriceId(User user, String priceId) {
        logger.info("Removing item from cart - User: {}, PriceId: {}", user.getId(), priceId);

        Cart cart = cartRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        boolean removed = cart.getItems().removeIf(item -> item.getPriceId().equals(priceId));
        if (!removed) {
            throw new RuntimeException("Item not found in cart: " + priceId);
        }

        cartRepo.save(cart);
        return getCartResponse(cart);
    }

    // ──────────────────────────────────────────────
    // Clear Cart
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, Object> clearCart(User user) {
        logger.info("Clearing cart for user: {}", user.getId());

        Cart cart = cartRepo.findByUserId(user.getId()).orElse(null);
        if (cart == null) {
            return Map.of("success", true, "message", "Cart is already empty");
        }

        cart.getItems().clear();
        cartRepo.save(cart);
        logger.info("Cart cleared for user: {}", user.getId());
        return Map.of("success", true, "message", "Cart cleared successfully");
    }

    // ──────────────────────────────────────────────
    // Merge Guest Cart
    // ──────────────────────────────────────────────
    @Transactional
    public Map<String, Object> mergeCarts(User user, String guestCartIdStr, String keepPreference) {
        logger.info("Merging carts - User: {}, GuestCartId: {}, KeepPreference: {}",
                user.getId(), guestCartIdStr, keepPreference);

        Long guestCartId;
        try {
            guestCartId = Long.parseLong(guestCartIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid guest cart ID: " + guestCartIdStr);
        }

        Cart userCart = cartRepo.findByUserId(user.getId()).orElse(null);
        Cart guestCart = cartRepo.findById(guestCartId).orElse(null);

        if (guestCart == null) {
            throw new IllegalArgumentException("Guest cart not found: " + guestCartIdStr);
        }
        if (guestCart.getUser() != null) {
            throw new IllegalArgumentException("Cart is not a guest cart: " + guestCartIdStr);
        }

        if (userCart == null) {
            guestCart.setUser(user);
            cartRepo.save(guestCart);
            return getCartResponse(guestCart);
        }

        String userCartType = getCartType(userCart);
        String guestCartType = getCartType(guestCart);

        if (!userCartType.equals(guestCartType) && keepPreference == null) {
            throw new RuntimeException("Cart type conflict: user_cart_type=" + userCartType +
                    ", guest_cart_type=" + guestCartType);
        }

        if (!userCartType.equals(guestCartType)) {
            if ("guest".equals(keepPreference)) {
                cartRepo.delete(userCart);
                guestCart.setUser(user);
                cartRepo.save(guestCart);
                return getCartResponse(guestCart);
            } else if ("user".equals(keepPreference)) {
                cartRepo.delete(guestCart);
                return getCartResponse(userCart);
            } else {
                throw new IllegalArgumentException("Invalid keep preference: " + keepPreference);
            }
        }

        mergeCartItems(userCart, guestCart);
        cartRepo.delete(guestCart);
        Cart saved = cartRepo.save(userCart);
        return getCartResponse(saved);
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private String getCartType(Cart cart) {
        if (cart.getItems().isEmpty()) return "empty";
        return cart.getItems().get(0).getType();
    }

    private void mergeCartItems(Cart userCart, Cart guestCart) {
        for (CartItem guestItem : guestCart.getItems()) {
            Optional<CartItem> existing = userCart.getItems().stream()
                    .filter(item -> item.getPriceId().equals(guestItem.getPriceId()))
                    .findFirst();
            if (existing.isPresent()) {
                existing.get().setQuantity(
                        existing.get().getQuantity() + guestItem.getQuantity());
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(userCart);
                newItem.setPriceId(guestItem.getPriceId());
                newItem.setQuantity(guestItem.getQuantity());
                newItem.setType(guestItem.getType());
                userCart.getItems().add(newItem);
            }
        }
    }

    private Map<String, Object> getCartResponse(Cart cart) {
        long subtotal = 0;
        for (CartItem item : cart.getItems()) {
            if ("charge".equals(item.getType())) {
                OneTimePrice p = oneTimePriceRepo.findById(item.getPriceId()).orElse(null);
                if (p != null) subtotal += p.getPrice() * item.getQuantity();
            } else if ("plan".equals(item.getType())) {
                SubscriptionPlan p = planRepo.findById(item.getPriceId()).orElse(null);
                if (p != null) subtotal += p.getPrice() * item.getQuantity();
            }
        }

        long deliveryFee = (subtotal >= 50000) ? 0 : 4000;
        long tax = 0;
        long grandTotal = subtotal + deliveryFee + tax;

        Map<String, Object> resp = new HashMap<>();
        resp.put("cart_id", cart.getId());
        resp.put("items", cart.getItems());
        resp.put("subtotal", subtotal);
        resp.put("delivery_fee", deliveryFee);
        resp.put("tax", tax);
        resp.put("grand_total", grandTotal);
        return resp;
    }
}
