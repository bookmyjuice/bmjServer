package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bookmyjuice.models.Cart;
import com.bookmyjuice.models.User;
import com.bookmyjuice.repository.CartRepository;
import com.bookmyjuice.repository.OneTimePriceRepository;
import com.bookmyjuice.repository.SubscriptionPlanRepository;

/**
 * Unit tests for CartService
 *
 * TC-CART-001: Get cart returns cart for existing user
 * TC-CART-002: Get cart creates empty cart for new user
 * TC-CART-003: Clear cart handles missing cart error
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepo;

    @Mock
    private OneTimePriceRepository oneTimePriceRepo;

    @Mock
    private SubscriptionPlanRepository planRepo;

    @InjectMocks
    private CartService cartService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("TC-CART-001: Get cart returns cart for existing user")
    void testGetCart_ExistingUser() {
        Cart cart = new Cart();
        cart.setId(100L);
        cart.setUser(testUser);
        cart.setItems(new ArrayList<>());

        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(cart));

        var result = cartService.getCart(testUser);

        assertNotNull(result);
        assertEquals(100L, result.get("cart_id"));
    }

    @Test
    @DisplayName("TC-CART-002: Get cart creates empty cart for new user")
    void testGetCart_NewUser() {
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.empty());
        Cart savedCart = new Cart();
        savedCart.setId(200L);
        savedCart.setUser(testUser);
        when(cartRepo.save(any(Cart.class))).thenReturn(savedCart);

        var result = cartService.getCart(testUser);

        assertNotNull(result);
        assertEquals(200L, result.get("cart_id"));
    }

    @Test
    @DisplayName("TC-CART-003: Add item throws when priceId is null")
    void testAddItem_NullPriceId() {
        assertThrows(RuntimeException.class, () -> cartService.addItem(testUser, null, 1));
    }

    @Test
    @DisplayName("TC-CART-004: Invalid price ID format throws")
    void testAddItem_InvalidFormat() {
        assertThrows(IllegalArgumentException.class,
            () -> cartService.addItem(testUser, "invalid_format", 1));
    }

    @Test
    @DisplayName("TC-CART-005: Get cart response has correct structure")
    void testGetCart_ResponseStructure() {
        Cart cart = new Cart();
        cart.setId(300L);
        cart.setUser(testUser);
        cart.setItems(new ArrayList<>());

        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(cart));

        var result = cartService.getCart(testUser);

        assertNotNull(result.get("cart_id"));
        assertNotNull(result.get("items"));
        assertNotNull(result.get("subtotal"));
        assertNotNull(result.get("delivery_fee"));
        assertNotNull(result.get("tax"));
        assertNotNull(result.get("grand_total"));
    }
}
