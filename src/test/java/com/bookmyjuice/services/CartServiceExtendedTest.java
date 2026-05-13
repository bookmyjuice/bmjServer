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

/**
 * Extended unit tests for CartService
 *
 * TC-CART-006: Clear cart — existing cart
 * TC-CART-007: Clear cart — no cart
 */
@ExtendWith(MockitoExtension.class)
class CartServiceExtendedTest {

    @Mock
    private CartRepository cartRepo;

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
    @DisplayName("TC-CART-006: Clear cart with existing cart returns success")
    void testClearCart_ExistingCart() {
        Cart cart = new Cart();
        cart.setId(100L);
        cart.setUser(testUser);
        cart.setItems(new ArrayList<>());

        when(cartRepo.findByUserId(1L)).thenReturn(Optional.of(cart));

        var result = cartService.clearCart(testUser);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals("Cart cleared successfully", result.get("message"));
    }

    @Test
    @DisplayName("TC-CART-007: Clear cart with no cart returns already empty")
    void testClearCart_NoCart() {
        when(cartRepo.findByUserId(1L)).thenReturn(Optional.empty());

        var result = cartService.clearCart(testUser);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals("Cart is already empty", result.get("message"));
    }
}
