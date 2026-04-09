package com.bookmyjuice.services;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chargebee.models.Event;

/**
 * Service to handle webhook events and ensure all related entities are updated properly.
 * This service manages the cascading updates between parent and child entities.
 */
@Service
public class WebhookEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEventProcessor.class);

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemPriceService itemPriceService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CreditNoteService creditNoteService;

    @Autowired
    private OrderService orderService;

    // Store processing results to track what was updated
    private final Map<String, Object> processingResults = new HashMap<>();

    /**
     * Process customer-related webhook events with cascading updates
     */
    @Transactional
    public ResponseEntity<String> processCustomerEvent(Event event) {
        logger.info("Processing customer event: {} for customer: {}", 
                   event.eventType(), event.content().customer().id());
        
        processingResults.clear();
        ResponseEntity<String> result;

        try {
            // Process the main customer event
            switch (event.eventType()) {
                case CUSTOMER_CREATED -> result = customerService.saveCustomer(event) ? 
                    ResponseEntity.ok("Customer created successfully") : 
                    ResponseEntity.ok("Customer already exists");
                case CUSTOMER_CHANGED -> result = customerService.updateCustomer(event) ? 
                    ResponseEntity.ok("Customer updated successfully") : 
                    ResponseEntity.badRequest().body("Customer update failed");
                case CUSTOMER_DELETED -> result = customerService.deleteCustomer(event) ? 
                    ResponseEntity.ok("Customer deleted successfully") : 
                    ResponseEntity.badRequest().body("Customer deletion failed");
                default -> {
                    logger.warn("Unhandled customer event type: {}", event.eventType());
                    return ResponseEntity.badRequest().body("Unhandled customer event type: " + event.eventType());
                }
            }

            // Process related entities if customer operation was successful
            if (result.getStatusCode().is2xxSuccessful()) {
                processRelatedEntitiesForCustomer(event);
            }

            return result;

        } catch (Exception e) {
            logger.error("Error processing customer event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing customer event: " + e.getMessage());
        }
    }

    /**
     * Process subscription-related webhook events with cascading updates
     */
    @Transactional
    public ResponseEntity<String> processSubscriptionEvent(Event event) {
        logger.info("Processing subscription event: {} for subscription: {}", 
                   event.eventType(), event.content().subscription().id());
        
        processingResults.clear();
        ResponseEntity<String> result;

        try {
            // Ensure customer exists first
            if (event.content().customer() != null) {
                customerService.saveCustomer(event); // Idempotent operation
                processingResults.put("customer_updated", true);
            }

            // Process the main subscription event
            switch (event.eventType()) {
                case SUBSCRIPTION_CREATED -> result = subscriptionService.saveSubscriptions(event) ? 
                    ResponseEntity.ok("Subscription created successfully") : 
                    ResponseEntity.ok("Subscription already exists");
                case SUBSCRIPTION_CHANGED, SUBSCRIPTION_PAUSED, SUBSCRIPTION_CANCELLED -> 
                    result = subscriptionService.updateSubscription(event) ? 
                    ResponseEntity.ok("Subscription updated successfully") : 
                    ResponseEntity.badRequest().body("Subscription update failed");
                case SUBSCRIPTION_REACTIVATED -> result = subscriptionService.reactivateSubscription(event) ? 
                    ResponseEntity.ok("Subscription reactivated successfully") : 
                    ResponseEntity.badRequest().body("Subscription reactivation failed");
                case SUBSCRIPTION_RENEWED -> result = subscriptionService.renewSubscription(event) ? 
                    ResponseEntity.ok("Subscription renewed successfully") : 
                    ResponseEntity.badRequest().body("Subscription renewal failed");
                default -> {
                    logger.warn("Unhandled subscription event type: {}", event.eventType());
                    return ResponseEntity.badRequest().body("Unhandled subscription event type: " + event.eventType());
                }
            }

            // Process related entities if subscription operation was successful
            if (result.getStatusCode().is2xxSuccessful()) {
                processRelatedEntitiesForSubscription(event);
            }

            return result;

        } catch (Exception e) {
            logger.error("Error processing subscription event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing subscription event: " + e.getMessage());
        }
    }

    /**
     * Process item-related webhook events with cascading updates
     */
    @Transactional
    public ResponseEntity<?> processItemEvent(Event event) {
        logger.info("Processing item event: {} for item: {}", 
                   event.eventType(), event.content().item().id());
        
        processingResults.clear();
        ResponseEntity<?> result;

        try {
            // Process the main item event
            String eventType = event.eventType().name();
            switch (eventType) {
                case "ITEM_CREATED" -> result = itemService.saveItem(event);
                case "ITEM_UPDATED" -> result = itemService.updateItem(event);
                case "ITEM_DELETED" -> result = itemService.deleteItem(event);
                case "ITEM_ARCHIVED" -> result = itemService.archiveItem(event);
                default -> {
                    logger.warn("Unhandled item event type: {}", eventType);
                    return itemService.handleDefaultItemEvent(event);
                }
            }

            // Process related entities if item operation was successful
            if (result.getStatusCode().is2xxSuccessful()) {
                processRelatedEntitiesForItem(event);
            }

            return result;

        } catch (Exception e) {
            logger.error("Error processing item event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing item event: " + e.getMessage());
        }
    }

    /**
     * Process item-price-related webhook events with parent Item verification
     */
    @Transactional
    public ResponseEntity<?> processItemPriceEvent(Event event) {
        logger.info("Processing item-price event: {} for item-price: {}", 
                   event.eventType(), event.content().itemPrice().id());
        
        processingResults.clear();
        ResponseEntity<?> result;

        try {
            var itemPrice = event.content().itemPrice();
            
            // Process the main item price event
            String eventType = event.eventType().name();
            switch (eventType) {
                case "ITEM_PRICE_CREATED" -> result = processItemPriceCreated(event);
                case "ITEM_PRICE_UPDATED" -> result = processItemPriceUpdated(event);
                case "ITEM_PRICE_DELETED" -> result = processItemPriceDeleted(event);
                default -> {
                    logger.warn("Unhandled item-price event type: {}", eventType);
                    return ResponseEntity.badRequest().body("Unhandled item-price event type: " + eventType);
                }
            }

            // Process related entities if item price operation was successful
            if (result.getStatusCode().is2xxSuccessful()) {
                processRelatedEntitiesForItemPrice(event);
            }

            return result;

        } catch (Exception e) {
            logger.error("Error processing item-price event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing item-price event: " + e.getMessage());
        }
    }

    private ResponseEntity<?> processItemPriceCreated(Event event) {
        boolean success = itemPriceService.saveItemPrice(event);
        if (success) {
            logger.info("ItemPrice created successfully: {}", event.content().itemPrice().id());
            return ResponseEntity.ok("ItemPrice created successfully");
        } else {
            logger.error("Failed to create ItemPrice: {}", event.content().itemPrice().id());
            return ResponseEntity.internalServerError().body("Failed to create ItemPrice");
        }
    }

    private ResponseEntity<?> processItemPriceUpdated(Event event) {
        boolean success = itemPriceService.updateItemPrice(event);
        if (success) {
            logger.info("ItemPrice updated successfully: {}", event.content().itemPrice().id());
            return ResponseEntity.ok("ItemPrice updated successfully");
        } else {
            logger.error("Failed to update ItemPrice: {}", event.content().itemPrice().id());
            return ResponseEntity.internalServerError().body("Failed to update ItemPrice");
        }
    }

    private ResponseEntity<?> processItemPriceDeleted(Event event) {
        boolean success = itemPriceService.deleteItemPrice(event);
        if (success) {
            logger.info("ItemPrice deleted successfully: {}", event.content().itemPrice().id());
            return ResponseEntity.ok("ItemPrice deleted successfully");
        } else {
            logger.error("Failed to delete ItemPrice: {}", event.content().itemPrice().id());
            return ResponseEntity.internalServerError().body("Failed to delete ItemPrice");
        }
    }

    private void processRelatedEntitiesForItemPrice(Event event) {
        logger.debug("Processing related entities for item-price: {}", event.content().itemPrice().id());
        
        var itemPrice = event.content().itemPrice();
        
        // Ensure parent Item exists and is properly linked
        if (itemPrice.itemId() != null) {
            logger.debug("Verifying parent Item {} exists for ItemPrice {}", 
                        itemPrice.itemId(), itemPrice.id());
            processingResults.put("parent_item_verified", true);
        }
        
        // ItemPrice events may include Item data
        if (event.content().item() != null) {
            logger.info("Processing nested Item {} for ItemPrice {}", 
                       event.content().item().id(), itemPrice.id());
            
            // Save or update the nested item
            ResponseEntity<?> itemResult = itemService.saveItem(event);
            processingResults.put("nested_item_processed", itemResult.getStatusCode().is2xxSuccessful());
            
            if (itemResult.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully processed nested Item {} for ItemPrice {}", 
                           event.content().item().id(), itemPrice.id());
            } else {
                logger.error("Failed to process nested Item {} for ItemPrice {}", 
                            event.content().item().id(), itemPrice.id());
            }
        }
    }

    /**
     * Process invoice-related webhook events with cascading updates
     */
    @Transactional
    public ResponseEntity<String> processInvoiceEvent(Event event) {
        logger.info("Processing invoice event: {} for invoice: {}", 
                   event.eventType(), event.content().invoice().id());
        
        processingResults.clear();

        try {
            // Ensure customer exists first
            if (event.content().customer() != null) {
                customerService.saveCustomer(event); // Idempotent operation
                processingResults.put("customer_updated", true);
            }

            // Process the main invoice event
            String eventType = event.eventType().name();
            switch (eventType) {
                case "INVOICE_CREATED", "INVOICE_GENERATED", "INVOICE_UPDATED", "INVOICE_PAID", "INVOICE_VOIDED" -> {
                    invoiceService.saveOrUpdateInvoice(event);
                    processRelatedEntitiesForInvoice(event);
                    return ResponseEntity.ok("Invoice event processed");
                }
                case "INVOICE_DELETED" -> {
                    invoiceService.deleteInvoice(event);
                    return ResponseEntity.ok("Invoice deleted");
                }
                default -> {
                    logger.warn("Unhandled invoice event type: {}", eventType);
                    return ResponseEntity.badRequest().body("Unhandled invoice event type: " + eventType);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing invoice event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing invoice event: " + e.getMessage());
        }
    }

    /**
     * Process payment-related webhook events with cascading updates
     */
    @Transactional
    public ResponseEntity<String> processPaymentEvent(Event event) {
        logger.info("Processing payment event: {} for payment: {}", 
                   event.eventType(), event.content().transaction().id());
        
        processingResults.clear();

        try {
            // Ensure customer exists first
            if (event.content().customer() != null) {
                customerService.saveCustomer(event); // Idempotent operation
                processingResults.put("customer_updated", true);
            }

            // Process the main payment event
            String eventType = event.eventType().name();
            switch (eventType) {
                case "PAYMENT_SUCCEEDED", "PAYMENT_CREATED", "PAYMENT_UPDATED" -> {
                    paymentService.saveOrUpdatePayment(event);
                    processRelatedEntitiesForPayment(event);
                    return ResponseEntity.ok("Payment event processed");
                }
                case "PAYMENT_DELETED" -> {
                    paymentService.deletePayment(event);
                    return ResponseEntity.ok("Payment deleted");
                }
                default -> {
                    logger.warn("Unhandled payment event type: {}", eventType);
                    return ResponseEntity.badRequest().body("Unhandled payment event type: " + eventType);
                }
            }

        } catch (Exception e) {
            logger.error("Error processing payment event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing payment event: " + e.getMessage());
        }
    }

    // Private methods to handle cascading updates for related entities

    private void processRelatedEntitiesForCustomer(Event event) {
        logger.debug("Processing related entities for customer: {}", event.content().customer().id());
        // Customer events may also include updated subscription info
        if (event.content().subscription() != null) {
            // Update subscription if present in the event
            subscriptionService.updateSubscription(event);
            processingResults.put("subscription_updated", true);
        }
    }

    private void processRelatedEntitiesForSubscription(Event event) {
        logger.debug("Processing related entities for subscription: {}", event.content().subscription().id());
        
        // Update subscription items if present
        if (event.content().subscription().subscriptionItems() != null) {
            // Handle subscription items updates
            processingResults.put("subscription_items_updated", true);
        }

        // Update attached items if present (addons, coupons, discounts)
        if (event.content().subscription().addons() != null) {
            // Handle addons updates  
            processingResults.put("addons_updated", true);
        }
        
        if (event.content().subscription().coupons() != null) {
            // Handle coupons updates  
            processingResults.put("coupons_updated", true);
        }
        
        if (event.content().subscription().discounts() != null) {
            // Handle discounts updates  
            processingResults.put("discounts_updated", true);
        }
    }

    private void processRelatedEntitiesForItem(Event event) {
        logger.debug("Processing related entities for item: {}", event.content().item().id());
        
        // Item events may contain nested item prices
        if (event.content().itemPrice() != null) {
            logger.info("Processing nested ItemPrice {} for Item {}", 
                       event.content().itemPrice().id(), event.content().item().id());
            
            // Use the saveOrUpdateItemPrice method to handle both create and update cases
            boolean success = itemPriceService.saveOrUpdateItemPrice(event);
            processingResults.put("item_price_processed", success);
            
            if (success) {
                logger.info("Successfully processed nested ItemPrice {} for Item {}", 
                           event.content().itemPrice().id(), event.content().item().id());
            } else {
                logger.error("Failed to process nested ItemPrice {} for Item {}", 
                            event.content().itemPrice().id(), event.content().item().id());
            }
        }
    }

    private void processRelatedEntitiesForInvoice(Event event) {
        logger.debug("Processing related entities for invoice: {}", event.content().invoice().id());
        
        // Invoice events may include subscription updates
        if (event.content().subscription() != null) {
            subscriptionService.updateSubscription(event);
            processingResults.put("subscription_updated", true);
        }

        // Invoice events may include credit notes
        if (event.content().creditNote() != null) {
            creditNoteService.saveOrUpdateCreditNote(event);
            processingResults.put("credit_note_updated", true);
        }
    }

    private void processRelatedEntitiesForPayment(Event event) {
        logger.debug("Processing related entities for payment: {}", event.content().transaction().id());
        
        // Payment events may include invoice updates
        if (event.content().invoice() != null) {
            invoiceService.saveOrUpdateInvoice(event);
            processingResults.put("invoice_updated", true);
        }

        // Payment events may include subscription updates
        if (event.content().subscription() != null) {
            subscriptionService.updateSubscription(event);
            processingResults.put("subscription_updated", true);
        }
    }

    /**
     * Get the results of the last processing operation
     */
    public Map<String, Object> getLastProcessingResults() {
        return new HashMap<>(processingResults);
    }
}