package com.bookmyjuice.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bookmyjuice.models.User;
import com.bookmyjuice.models.dto.BottleLedgerEntry;
import com.bookmyjuice.models.entities.BottleTransactionEntity;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.services.BottleTrackingService;
import com.bookmyjuice.services.UserDetailsImpl;

/**
 * Unit tests for BottleTrackingController.
 *
 * Covers all 4 endpoints: GET /ledger, GET /transactions, POST /return, POST /broken.
 * Follows the same pattern as AuthControllerTest.
 * References: docs/use-cases/UC-BOTTLE-TRACKING.md
 */
class BottleTrackingControllerTest {

    @Mock
    private BottleTrackingService bottleTrackingService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BottleTrackingController bottleTrackingController;

    private User testUser;
    private UserDetailsImpl testUserDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User("testuser", "test@example.com", "hashedPassword");
        testUser.setId(100L);
        testUser.setChargebeeCustomerId("cb_cus_test123");

        testUserDetails = new UserDetailsImpl(
            100L, "testuser", "test@example.com", "hashedPassword",
            List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
    }

    /**
     * Helper to set up authenticated SecurityContext with testUser and testUserDetails.
     * Must be paired with clearSecurityContext() in @AfterEach or at end of test.
     */
    private void setupAuthenticatedUser() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(testUserDetails);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
    }

    // ============================================================
    // Nested: getLedger tests
    // ============================================================
    @Nested
    @DisplayName("GET /api/bottles/ledger tests")
    class GetLedgerTests {

        @Test
        @DisplayName("Should return 200 with ledger data for authenticated user")
        void testGetLedger_Success() {
            setupAuthenticatedUser();

            List<BottleLedgerEntry> ledger = new ArrayList<>();
            ledger.add(new BottleLedgerEntry(
                "cb_cus_test123", "glass_500ml", 10, 4, 1, new Date()));

            when(bottleTrackingService.getLedger("cb_cus_test123")).thenReturn(ledger);

            ResponseEntity<?> response = bottleTrackingController.getLedger();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertNotNull(body);
            assertEquals("success", body.get("status"));
            assertNotNull(body.get("data"));

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated (null authentication)")
        void testGetLedger_NotAuthenticated() {
            SecurityContextHolder.clearContext();

            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            // Mock anonymous authentication
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);
            // getPrincipal() returns something that is not UserDetailsImpl
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            ResponseEntity<?> response = bottleTrackingController.getLedger();

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertNotNull(body);
            assertTrue(body.containsKey("error"));

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 400 when user has no Chargebee customer ID")
        void testGetLedger_NoChargebeeId() {
            testUser.setChargebeeCustomerId(null);
            setupAuthenticatedUser();

            ResponseEntity<?> response = bottleTrackingController.getLedger();

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertNotNull(body);
            assertTrue(body.get("error").toString().contains("Chargebee customer ID not found"));

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should handle service exception gracefully")
        void testGetLedger_ServiceException() {
            setupAuthenticatedUser();
            when(bottleTrackingService.getLedger("cb_cus_test123"))
                .thenThrow(new RuntimeException("DB error"));

            ResponseEntity<?> response = bottleTrackingController.getLedger();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

            SecurityContextHolder.clearContext();
        }
    }

    // ============================================================
    // Nested: getTransactions tests
    // ============================================================
    @Nested
    @DisplayName("GET /api/bottles/transactions tests")
    class GetTransactionsTests {

        @Test
        @DisplayName("Should return 200 with transactions for authenticated user")
        void testGetTransactions_Success() {
            setupAuthenticatedUser();

            List<BottleTransactionEntity> txs = new ArrayList<>();
            BottleTransactionEntity tx = new BottleTransactionEntity();
            tx.setId(1L);
            tx.setOrderId("cb_order_001");
            tx.setBottleType("glass_500ml");
            tx.setQuantity(5);
            tx.setAction("ISSUED");
            txs.add(tx);

            when(bottleTrackingService.getTransactions("cb_cus_test123")).thenReturn(txs);

            ResponseEntity<?> response = bottleTrackingController.getTransactions();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertNotNull(body);
            assertEquals("success", body.get("status"));
            assertEquals(1, body.get("count"));

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void testGetTransactions_NotAuthenticated() {
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            ResponseEntity<?> response = bottleTrackingController.getTransactions();

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

            SecurityContextHolder.clearContext();
        }
    }

    // ============================================================
    // Nested: recordReturn tests
    // ============================================================
    @Nested
    @DisplayName("POST /api/bottles/return tests")
    class RecordReturnTests {

        @Test
        @DisplayName("Should return 200 on successful return recording")
        void testRecordReturn_Success() {
            setupAuthenticatedUser();

            Map<String, Object> body = Map.of(
                "orderId", "cb_order_001",
                "bottleType", "glass_500ml",
                "quantity", 3);

            BottleTransactionEntity tx = new BottleTransactionEntity();
            tx.setId(10L);
            tx.setOrderId("cb_order_001");
            tx.setBottleType("glass_500ml");
            tx.setQuantity(3);
            tx.setAction("RETURNED");

            when(bottleTrackingService.recordReturn(
                anyString(), anyString(), anyString(), anyInt(), any(), anyString()))
                .thenReturn(tx);

            ResponseEntity<?> response = bottleTrackingController.recordReturn(body);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> respBody = (Map<String, Object>) response.getBody();
            assertNotNull(respBody);
            assertEquals("success", respBody.get("status"));

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 400 when required params are missing")
        void testRecordReturn_MissingParams() {
            setupAuthenticatedUser();

            Map<String, Object> body = Map.of("orderId", "cb_order_001");

            ResponseEntity<?> response = bottleTrackingController.recordReturn(body);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 400 when quantity is zero or negative")
        void testRecordReturn_InvalidQuantity() {
            setupAuthenticatedUser();

            Map<String, Object> body = Map.of(
                "orderId", "cb_order_001",
                "bottleType", "glass_500ml",
                "quantity", 0);

            ResponseEntity<?> response = bottleTrackingController.recordReturn(body);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            SecurityContextHolder.clearContext();
        }
    }

    // ============================================================
    // Nested: recordBroken tests
    // ============================================================
    @Nested
    @DisplayName("POST /api/bottles/broken tests")
    class RecordBrokenTests {

        @Test
        @DisplayName("Should return 200 on successful broken recording")
        void testRecordBroken_Success() {
            setupAuthenticatedUser();

            Map<String, Object> body = Map.of(
                "orderId", "cb_order_002",
                "bottleType", "glass_500ml",
                "quantity", 2);

            BottleTransactionEntity tx = new BottleTransactionEntity();
            tx.setId(11L);
            tx.setOrderId("cb_order_002");
            tx.setBottleType("glass_500ml");
            tx.setQuantity(2);
            tx.setAction("BROKEN");

            when(bottleTrackingService.recordBroken(
                anyString(), anyString(), anyString(), anyInt(), any(), anyString()))
                .thenReturn(tx);

            ResponseEntity<?> response = bottleTrackingController.recordBroken(body);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, Object> respBody = (Map<String, Object>) response.getBody();
            assertNotNull(respBody);
            assertEquals("success", respBody.get("status"));

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 400 when required params are missing")
        void testRecordBroken_MissingParams() {
            setupAuthenticatedUser();

            Map<String, Object> body = Map.of("bottleType", "glass_500ml");

            ResponseEntity<?> response = bottleTrackingController.recordBroken(body);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void testRecordBroken_NotAuthenticated() {
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            Map<String, Object> body = Map.of(
                "orderId", "cb_order_002",
                "bottleType", "glass_500ml",
                "quantity", 2);

            ResponseEntity<?> response = bottleTrackingController.recordBroken(body);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

            SecurityContextHolder.clearContext();
        }
    }
}
