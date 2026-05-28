package com.bookmyjuice.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.dto.BottleLedgerEntry;
import com.bookmyjuice.models.entities.BottleTransactionEntity;
import com.bookmyjuice.repository.BottleTransactionRepository;

/**
 * Service for bottle tracking operations.
 * 
 * Manages the lifecycle (issue, return, broken/lost reporting) of
 * reusable bottles via the bottle_transactions table.
 * Provides a computed BottleLedger view showing per-customer balances.
 */
@Service
public class BottleTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(BottleTrackingService.class);

    @Autowired
    private BottleTransactionRepository bottleTransactionRepo;

    // ==================== Record Operations ====================

    /**
     * Record bottles issued to a customer as part of an order or subscription delivery.
     */
    @Transactional
    public BottleTransactionEntity recordIssue(String orderId, String customerId,
                                                String bottleType, int quantity,
                                                String referenceId, String notes) {
        logger.info("Recording bottle ISSUE: order={}, customer={}, type={}, qty={}",
            orderId, customerId, bottleType, quantity);

        BottleTransactionEntity tx = new BottleTransactionEntity();
        tx.setOrderId(orderId);
        tx.setCustomerId(customerId);
        tx.setBottleType(bottleType);
        tx.setQuantity(quantity);
        tx.setAction("ISSUED");
        tx.setReferenceId(referenceId);
        tx.setNotes(notes);

        BottleTransactionEntity saved = bottleTransactionRepo.save(tx);
        logger.debug("Recorded bottle transaction id={}, action=ISSUED", saved.getId());
        return saved;
    }

    /**
     * Record bottles returned by a customer (e.g., at delivery pickup).
     */
    @Transactional
    public BottleTransactionEntity recordReturn(String orderId, String customerId,
                                                  String bottleType, int quantity,
                                                  String referenceId, String notes) {
        logger.info("Recording bottle RETURN: order={}, customer={}, type={}, qty={}",
            orderId, customerId, bottleType, quantity);

        BottleTransactionEntity tx = new BottleTransactionEntity();
        tx.setOrderId(orderId);
        tx.setCustomerId(customerId);
        tx.setBottleType(bottleType);
        tx.setQuantity(quantity);
        tx.setAction("RETURNED");
        tx.setReferenceId(referenceId);
        tx.setNotes(notes);

        BottleTransactionEntity saved = bottleTransactionRepo.save(tx);
        logger.debug("Recorded bottle transaction id={}, action=RETURNED", saved.getId());
        return saved;
    }

    /**
     * Record bottles reported broken or lost by a customer.
     */
    @Transactional
    public BottleTransactionEntity recordBroken(String orderId, String customerId,
                                                  String bottleType, int quantity,
                                                  String referenceId, String notes) {
        logger.info("Recording bottle BROKEN: order={}, customer={}, type={}, qty={}",
            orderId, customerId, bottleType, quantity);

        BottleTransactionEntity tx = new BottleTransactionEntity();
        tx.setOrderId(orderId);
        tx.setCustomerId(customerId);
        tx.setBottleType(bottleType);
        tx.setQuantity(quantity);
        tx.setAction("BROKEN");
        tx.setReferenceId(referenceId);
        tx.setNotes(notes);

        BottleTransactionEntity saved = bottleTransactionRepo.save(tx);
        logger.debug("Recorded bottle transaction id={}, action=BROKEN", saved.getId());
        return saved;
    }

    // ==================== Query Operations ====================

    /**
     * Get the computed BottleLedger for a specific customer.
     * Returns one entry per bottle type with running balances.
     */
    public List<BottleLedgerEntry> getLedger(String customerId) {
        logger.info("Computing bottle ledger for customer: {}", customerId);

        List<Object[]> aggregates = bottleTransactionRepo.aggregateByCustomer(customerId);
        Map<String, BottleLedgerEntry.Builder> builderMap = new HashMap<>();

        // Find last transaction date per bottle type
        List<BottleTransactionEntity> recentTxs = bottleTransactionRepo
                .findByCustomerIdOrderByCreatedAtDesc(customerId);
        Map<String, Date> lastTxDateMap = new HashMap<>();
        for (BottleTransactionEntity tx : recentTxs) {
            lastTxDateMap.putIfAbsent(tx.getBottleType(), tx.getCreatedAt());
        }

        for (Object[] row : aggregates) {
            String bottleType = (String) row[1];
            String action = (String) row[2];
            int sum = ((Number) row[3]).intValue();

            BottleLedgerEntry.Builder builder = builderMap.computeIfAbsent(
                bottleType, k -> new BottleLedgerEntry.Builder(customerId, bottleType));

            switch (action) {
                case "ISSUED"   -> builder.addIssued(sum);
                case "RETURNED" -> builder.addReturned(sum);
                case "BROKEN"   -> builder.addBroken(sum);
                default -> logger.warn("Unknown bottle action: {}", action);
            }
        }

        List<BottleLedgerEntry> ledger = new ArrayList<>();
        for (BottleLedgerEntry.Builder builder : builderMap.values()) {
            Date lastTx = lastTxDateMap.get(builder.getBottleType());
            ledger.add(builder.withLastTransactionAt(lastTx).build());
        }

        logger.debug("Ledger computed: {} entries for customer {}", ledger.size(), customerId);
        return ledger;
    }

    /**
     * Get raw transaction history for a customer.
     */
    public List<BottleTransactionEntity> getTransactions(String customerId) {
        return bottleTransactionRepo.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Get raw transaction history for a specific order.
     */
    public List<BottleTransactionEntity> getOrderTransactions(String orderId) {
        return bottleTransactionRepo.findByOrderIdOrderByCreatedAtDesc(orderId);
    }

    // ==================== Auto-dispatch ====================

    /**
     * Non-blocking auto-dispatch called after payment confirmation.
     * Records bottle issuance based on order line items.
     * This is a best-effort operation — failures are logged but not propagated.
     * 
     * @param orderId    The Chargebee order ID
     * @param customerId The Chargebee customer ID
     * @param orderLineItemsJson JSON string of order line items (parsed to determine bottle types)
     */
    public void autoDispatchBottles(String orderId, String customerId, String orderLineItemsJson) {
        try {
            logger.info("Auto-dispatching bottles for order={}, customer={}", orderId, customerId);

            // Parse order line items to determine bottle types and quantities
            // Default logic: each order line item represents 1 bottle of default type
            // In production, this should parse the JSON to extract item-specific bottle types
            int itemCount = 1;

            if (orderLineItemsJson != null && !orderLineItemsJson.isBlank()) {
                // Simple heuristic: count items in the JSON array
                itemCount = Math.max(1, orderLineItemsJson.split("\\{").length - 1);
            }

            int defaultBottleQty = Math.min(itemCount, 20); // Sanity cap at 20

            if (defaultBottleQty > 0) {
                recordIssue(orderId, customerId, "glass_500ml",
                    defaultBottleQty, "auto-dispatch", 
                    "Auto-dispatched on payment confirmation");
                logger.info("Auto-dispatch complete: {} bottle(s) issued for order {}",
                    defaultBottleQty, orderId);
            }
        } catch (Exception e) {
            // Never let bottle tracking failure break the checkout flow
            logger.error("Auto-dispatch failed for order {}: {}", orderId, e.getMessage(), e);
        }
    }

}
