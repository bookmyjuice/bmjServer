package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.bookmyjuice.models.dto.BottleLedgerEntry;
import com.bookmyjuice.models.entities.BottleTransactionEntity;
import com.bookmyjuice.repository.BottleTransactionRepository;

/**
 * Unit tests for BottleTrackingService.
 *
 * Covers all record operations, ledger computation, queries, and auto-dispatch.
 * References: docs/use-cases/UC-BOTTLE-TRACKING.md
 */
class BottleTrackingServiceTest {

    @Mock
    private BottleTransactionRepository bottleTransactionRepo;

    @InjectMocks
    private BottleTrackingService bottleTrackingService;

    private BottleTransactionEntity sampleIssue;
    private BottleTransactionEntity sampleReturn;
    private BottleTransactionEntity sampleBroken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sampleIssue = new BottleTransactionEntity();
        sampleIssue.setId(1L);
        sampleIssue.setOrderId("cb_order_001");
        sampleIssue.setCustomerId("cb_cus_test123");
        sampleIssue.setBottleType("glass_500ml");
        sampleIssue.setQuantity(5);
        sampleIssue.setAction("ISSUED");
        sampleIssue.setReferenceId("auto-dispatch");
        sampleIssue.setNotes("Test issue");
        sampleIssue.setCreatedAt(new Date());
        sampleIssue.setUpdatedAt(new Date());

        sampleReturn = new BottleTransactionEntity();
        sampleReturn.setId(2L);
        sampleReturn.setOrderId("cb_order_001");
        sampleReturn.setCustomerId("cb_cus_test123");
        sampleReturn.setBottleType("glass_500ml");
        sampleReturn.setQuantity(2);
        sampleReturn.setAction("RETURNED");
        sampleReturn.setReferenceId(null);
        sampleReturn.setNotes("Test return");

        sampleBroken = new BottleTransactionEntity();
        sampleBroken.setId(3L);
        sampleBroken.setOrderId("cb_order_001");
        sampleBroken.setCustomerId("cb_cus_test123");
        sampleBroken.setBottleType("glass_500ml");
        sampleBroken.setQuantity(1);
        sampleBroken.setAction("BROKEN");
        sampleBroken.setReferenceId(null);
        sampleBroken.setNotes("Test broken");
    }

    // ============================================================
    // Nested: recordIssue tests
    // ============================================================
    @Nested
    @DisplayName("recordIssue tests")
    class RecordIssueTests {

        @Test
        @DisplayName("Should save issued bottle transaction successfully")
        void testRecordIssue_Success() {
            when(bottleTransactionRepo.save(any(BottleTransactionEntity.class)))
                .thenReturn(sampleIssue);

            BottleTransactionEntity result = bottleTrackingService.recordIssue(
                "cb_order_001", "cb_cus_test123", "glass_500ml", 5,
                "auto-dispatch", "Test issue");

            assertNotNull(result);
            assertEquals("ISSUED", result.getAction());
            assertEquals(5, result.getQuantity());
            assertEquals("glass_500ml", result.getBottleType());

            ArgumentCaptor<BottleTransactionEntity> captor =
                ArgumentCaptor.forClass(BottleTransactionEntity.class);
            verify(bottleTransactionRepo, times(1)).save(captor.capture());

            BottleTransactionEntity saved = captor.getValue();
            assertEquals("ISSUED", saved.getAction());
            assertEquals("cb_cus_test123", saved.getCustomerId());
        }
    }

    // ============================================================
    // Nested: recordReturn tests
    // ============================================================
    @Nested
    @DisplayName("recordReturn tests")
    class RecordReturnTests {

        @Test
        @DisplayName("Should save returned bottle transaction successfully")
        void testRecordReturn_Success() {
            when(bottleTransactionRepo.save(any(BottleTransactionEntity.class)))
                .thenReturn(sampleReturn);

            BottleTransactionEntity result = bottleTrackingService.recordReturn(
                "cb_order_001", "cb_cus_test123", "glass_500ml", 2,
                null, "Test return");

            assertNotNull(result);
            assertEquals("RETURNED", result.getAction());
            assertEquals(2, result.getQuantity());
        }
    }

    // ============================================================
    // Nested: recordBroken tests
    // ============================================================
    @Nested
    @DisplayName("recordBroken tests")
    class RecordBrokenTests {

        @Test
        @DisplayName("Should save broken bottle transaction successfully")
        void testRecordBroken_Success() {
            when(bottleTransactionRepo.save(any(BottleTransactionEntity.class)))
                .thenReturn(sampleBroken);

            BottleTransactionEntity result = bottleTrackingService.recordBroken(
                "cb_order_001", "cb_cus_test123", "glass_500ml", 1,
                null, "Test broken");

            assertNotNull(result);
            assertEquals("BROKEN", result.getAction());
            assertEquals(1, result.getQuantity());
        }
    }

    // ============================================================
    // Nested: getLedger tests
    // ============================================================
    @Nested
    @DisplayName("getLedger tests")
    class GetLedgerTests {

        @Test
        @DisplayName("Should compute correct ledger from aggregates")
        void testGetLedger_WithMultipleActions() {
            // Mock aggregate query results: Object[] = {customerId, bottleType, action, SUM(quantity)}
            List<Object[]> aggregates = new ArrayList<>();
            aggregates.add(new Object[]{"cb_cus_test123", "glass_500ml", "ISSUED", 10L});
            aggregates.add(new Object[]{"cb_cus_test123", "glass_500ml", "RETURNED", 4L});
            aggregates.add(new Object[]{"cb_cus_test123", "glass_500ml", "BROKEN", 1L});
            aggregates.add(new Object[]{"cb_cus_test123", "plastic_1l", "ISSUED", 3L});

            when(bottleTransactionRepo.aggregateByCustomer("cb_cus_test123"))
                .thenReturn(aggregates);

            // Mock recent transactions (for lastTransactionAt)
            List<BottleTransactionEntity> recentTxs = new ArrayList<>();
            recentTxs.add(sampleIssue);
            recentTxs.add(sampleReturn);
            recentTxs.add(sampleBroken);
            when(bottleTransactionRepo.findByCustomerIdOrderByCreatedAtDesc("cb_cus_test123"))
                .thenReturn(recentTxs);

            List<BottleLedgerEntry> ledger = bottleTrackingService.getLedger("cb_cus_test123");

            assertNotNull(ledger);
            assertEquals(2, ledger.size(), "Should have entries for 2 bottle types");

            // Find the glass_500ml entry
            BottleLedgerEntry glassEntry = ledger.stream()
                .filter(e -> "glass_500ml".equals(e.getBottleType()))
                .findFirst().orElse(null);
            assertNotNull(glassEntry);
            assertEquals(10, glassEntry.getTotalIssued());
            assertEquals(4, glassEntry.getTotalReturned());
            assertEquals(1, glassEntry.getTotalBroken());
            assertEquals(5, glassEntry.getOutstanding(), "10 - 4 - 1 = 5 outstanding");

            // Find the plastic_1l entry
            BottleLedgerEntry plasticEntry = ledger.stream()
                .filter(e -> "plastic_1l".equals(e.getBottleType()))
                .findFirst().orElse(null);
            assertNotNull(plasticEntry);
            assertEquals(3, plasticEntry.getTotalIssued());
            assertEquals(0, plasticEntry.getTotalReturned());
            assertEquals(0, plasticEntry.getTotalBroken());
            assertEquals(3, plasticEntry.getOutstanding());
        }

        @Test
        @DisplayName("Should return empty ledger for customer with no transactions")
        void testGetLedger_Empty() {
            when(bottleTransactionRepo.aggregateByCustomer("unknown_cus"))
                .thenReturn(new ArrayList<>());
            when(bottleTransactionRepo.findByCustomerIdOrderByCreatedAtDesc("unknown_cus"))
                .thenReturn(new ArrayList<>());

            List<BottleLedgerEntry> ledger = bottleTrackingService.getLedger("unknown_cus");

            assertNotNull(ledger);
            assertTrue(ledger.isEmpty(), "Ledger should be empty for unknown customer");
        }
    }

    // ============================================================
    // Nested: getTransactions tests
    // ============================================================
    @Nested
    @DisplayName("getTransactions tests")
    class GetTransactionsTests {

        @Test
        @DisplayName("Should return all transactions for a customer")
        void testGetTransactions() {
            List<BottleTransactionEntity> txs = List.of(sampleIssue, sampleReturn, sampleBroken);
            when(bottleTransactionRepo.findByCustomerIdOrderByCreatedAtDesc("cb_cus_test123"))
                .thenReturn(txs);

            List<BottleTransactionEntity> result = bottleTrackingService.getTransactions("cb_cus_test123");

            assertEquals(3, result.size());
            verify(bottleTransactionRepo, times(1))
                .findByCustomerIdOrderByCreatedAtDesc("cb_cus_test123");
        }

        @Test
        @DisplayName("Should return empty list for customer with no transactions")
        void testGetTransactions_Empty() {
            when(bottleTransactionRepo.findByCustomerIdOrderByCreatedAtDesc("empty_cus"))
                .thenReturn(new ArrayList<>());

            List<BottleTransactionEntity> result = bottleTrackingService.getTransactions("empty_cus");

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ============================================================
    // Nested: getOrderTransactions tests
    // ============================================================
    @Nested
    @DisplayName("getOrderTransactions tests")
    class GetOrderTransactionsTests {

        @Test
        @DisplayName("Should return all transactions for a specific order")
        void testGetOrderTransactions() {
            when(bottleTransactionRepo.findByOrderIdOrderByCreatedAtDesc("cb_order_001"))
                .thenReturn(List.of(sampleIssue, sampleReturn));

            List<BottleTransactionEntity> result =
                bottleTrackingService.getOrderTransactions("cb_order_001");

            assertEquals(2, result.size());
            for (BottleTransactionEntity tx : result) {
                assertEquals("cb_order_001", tx.getOrderId());
            }
        }
    }

    // ============================================================
    // Nested: autoDispatchBottles tests
    // ============================================================
    @Nested
    @DisplayName("autoDispatchBottles tests")
    class AutoDispatchBottlesTests {

        @Test
        @DisplayName("Should issue bottle for simple line items JSON")
        void testAutoDispatch_Simple() {
            String itemsJson = "[{\"id\":\"item_1\",\"quantity\":1}]";

            when(bottleTransactionRepo.save(any(BottleTransactionEntity.class)))
                .thenAnswer(invocation -> {
                    BottleTransactionEntity entity = invocation.getArgument(0);
                    entity.setId(10L);
                    return entity;
                });

            bottleTrackingService.autoDispatchBottles(
                "cb_order_002", "cb_cus_test456", itemsJson);

            ArgumentCaptor<BottleTransactionEntity> captor =
                ArgumentCaptor.forClass(BottleTransactionEntity.class);
            verify(bottleTransactionRepo, times(1)).save(captor.capture());

            BottleTransactionEntity saved = captor.getValue();
            assertEquals("ISSUED", saved.getAction());
            assertEquals("auto-dispatch", saved.getReferenceId());
            assertEquals("glass_500ml", saved.getBottleType());
            assertEquals(1, saved.getQuantity(), "Should dispatch 1 bottle for 1 item");
        }

        @Test
        @DisplayName("Should dispatch multiple bottles for multiple items")
        void testAutoDispatch_MultipleItems() {
            String itemsJson = "[{\"id\":\"a\",\"quantity\":1},{\"id\":\"b\",\"quantity\":2}]";

            when(bottleTransactionRepo.save(any(BottleTransactionEntity.class)))
                .thenAnswer(invocation -> {
                    BottleTransactionEntity entity = invocation.getArgument(0);
                    entity.setId(11L);
                    return entity;
                });

            bottleTrackingService.autoDispatchBottles(
                "cb_order_003", "cb_cus_test789", itemsJson);

            ArgumentCaptor<BottleTransactionEntity> captor =
                ArgumentCaptor.forClass(BottleTransactionEntity.class);
            verify(bottleTransactionRepo, times(1)).save(captor.capture());

            assertEquals(2, captor.getValue().getQuantity(),
                "Should dispatch 2 bottles for 2 items (split by '{' count)");
        }

        @Test
        @DisplayName("Should cap at 20 bottles maximum")
        void testAutoDispatch_CapAt20() {
            // Generate 30 items in JSON
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < 30; i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"id\":\"item_").append(i).append("\",\"quantity\":1}");
            }
            sb.append("]");

            when(bottleTransactionRepo.save(any(BottleTransactionEntity.class)))
                .thenAnswer(invocation -> {
                    BottleTransactionEntity entity = invocation.getArgument(0);
                    entity.setId(12L);
                    return entity;
                });

            bottleTrackingService.autoDispatchBottles(
                "cb_order_004", "cb_cus_test000", sb.toString());

            ArgumentCaptor<BottleTransactionEntity> captor =
                ArgumentCaptor.forClass(BottleTransactionEntity.class);
            verify(bottleTransactionRepo, times(1)).save(captor.capture());

            assertEquals(20, captor.getValue().getQuantity(),
                "Should cap at 20 bottles even with 30 items");
        }

        @Test
        @DisplayName("Should not throw exception on null JSON")
        void testAutoDispatch_NullJson() {
            bottleTrackingService.autoDispatchBottles(
                "cb_order_005", "cb_cus_test111", null);

            verify(bottleTransactionRepo, times(1)).save(any(BottleTransactionEntity.class));
        }

        @Test
        @DisplayName("Should handle repository exception gracefully (non-blocking)")
        void testAutoDispatch_ExceptionHandling() {
            when(bottleTransactionRepo.save(any(BottleTransactionEntity.class)))
                .thenThrow(new RuntimeException("DB error"));

            // Should not propagate exception (non-blocking best-effort)
            assertDoesNotThrow(() -> {
                bottleTrackingService.autoDispatchBottles(
                    "cb_order_006", "cb_cus_test222", "[{\"id\":\"x\"}]");
            });

            verify(bottleTransactionRepo, times(1)).save(any(BottleTransactionEntity.class));
        }
    }
}
