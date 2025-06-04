package com.bookmyjuice.controllers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.services.CustomerService;
import com.bookmyjuice.services.ItemService;
import com.bookmyjuice.services.SubscriptionService;
import com.bookmyjuice.services.InvoiceService;
import com.bookmyjuice.services.PaymentService;
import com.bookmyjuice.services.CreditNoteService;
import com.bookmyjuice.services.OrderService;
import com.chargebee.models.Event;


@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private CreditNoteService creditNoteService;
    @Autowired
    private OrderService orderService;

    // Idempotency tracking
    private final Map<String, Boolean> processedEvents = new ConcurrentHashMap<>();

    @PostMapping("/subscriptions")
    @ResponseBody
    public ResponseEntity<String> handleSubscriptionWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();

            // Idempotency check
            if (processedEvents.containsKey(event.id())) {
                if(subscriptionService.existsByCustomerId(event.content().subscription().customerId())){
                    logger.info("Subscription event already processed for customer ID: {}", event.content().subscription().customerId());
                     return ResponseEntity.status(200).body("Subscription-creation already processed");
                } else {
                    logger.warn("Subscription event already processed but no subscription found for customer ID: {}", event.content().subscription().customerId());
                   
                }
            }
            processedEvents.put(event.id(), true);
            // Add debugging code to log the return type of eventType()
            logger.debug("Event type: {}", event.eventType().getClass().getName());
            // Add debugging code to log the value of eventType()
            logger.debug("Event type value: {}", event.eventType());
            
            switch (event.eventType()) {
                case SUBSCRIPTION_CREATED -> {
                    return handleSubscriptionCreated(event);
                }
                case SUBSCRIPTION_CANCELLED -> {
                    return handleSubscriptionCancelled(event);
                }
                case SUBSCRIPTION_PAUSED -> {
                    return handleSubscriptionPaused(event);
                }
                case SUBSCRIPTION_REACTIVATED -> {
                    return handleSubscriptionReactivated(event);
                }
                case SUBSCRIPTION_RENEWED -> {
                    return handleSubscriptionRenewed(event);
                }
                case SUBSCRIPTION_CHANGED -> {
                    return ResponseEntity.status(200).body("Subscription changed event received");
                }
                default -> {
                    return handleDefaultSubscriptionEvent(event);
                }
            }
        } catch (Exception ex) {
            logger.error("Error processing subscription webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error processing subscription webhook: " + ex.getMessage());
        }
    }

    @PostMapping("/customers")
    @ResponseBody
    public ResponseEntity<String> handleCustomerWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();

            // Idempotency check
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Event already processed");
            }
            processedEvents.put(event.id(), true);

            switch (event.eventType()) {
                case CUSTOMER_CREATED -> {
                    return handleCustomerCreated(event);
                }
                case CUSTOMER_CHANGED -> {
                    return handleCustomerUpdated(event);
                }
                case CUSTOMER_DELETED -> {
                    return handleCustomerDeleted(event);
                }
                // case CUSTOMER_MIGRATED -> {
                //     return handleCustomerMigrated(event);
                // }
                // case CUSTOMER_BILLING_ADDRESS_UPDATED -> {
                //     return handleCustomerBillingAddressUpdated(event);
                // }
                default -> {
                    return handleDefaultCustomerEvent(event);
                }
            }
        } catch (Exception ex) {
            logger.error("Error processing customer webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error processing customer webhook: " + ex.getMessage());
        }
    }

    @PostMapping("/items")
    @ResponseBody
    public ResponseEntity<?> handleItemWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();

            // Idempotency check
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Event already processed");
            }
            processedEvents.put(event.id(), true);

            switch (event.eventType()) {
                case ITEM_CREATED -> {
                    return itemService.saveItem(event);
                }
                case ITEM_UPDATED -> {
                    return itemService.updateItem(event);
                }
                case ITEM_DELETED -> {
                    return itemService.deleteItem(event);
                }
                // case ITEM_ARCHIVED -> {
                //     return itemService.archiveItem(event);
                // }
                default -> {
                    return itemService.handleDefaultItemEvent(event);
                }
            }
        } catch (Exception ex) {
            logger.error("Error processing item webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error processing item webhook: " + ex.getMessage());
        }
    }

    private ResponseEntity<String> handleSubscriptionCreated(Event event) {
        if (subscriptionService.saveSubscriptions(event)) {
            return ResponseEntity.status(200).body("Subscription saved successfully!");
        } else {
            return ResponseEntity.status(200).body("Subscription already exists");
        }
    }

    private ResponseEntity<String> handleSubscriptionCancelled(Event event) {
        if (subscriptionService.updateSubscription(event)) {
            return ResponseEntity.status(200).body("Subscription cancelled successfully");
        } else {
            return ResponseEntity.status(400).body("Subscription not updated!");
        }
    }

    private ResponseEntity<String> handleSubscriptionPaused(Event event) {
        if (subscriptionService.updateSubscription(event)) {
            return ResponseEntity.status(200).body("Subscription paused successfully");
        } else {
            return ResponseEntity.status(400).body("Subscription not updated!");
        }
    }

    private ResponseEntity<String> handleSubscriptionReactivated(Event event) {
        if (subscriptionService.reactivateSubscription(event)) {
            return ResponseEntity.status(200).body("Subscription reactivated successfully!");
        } else {
            return ResponseEntity.status(400).body("Subscription not reactivated!");
        }
    }

    private ResponseEntity<String> handleSubscriptionRenewed(Event event) {
        if (subscriptionService.renewSubscription(event)) {
            return ResponseEntity.status(200).body("Subscription renewed successfully!");
        } else {
            return ResponseEntity.status(400).body("Subscription not renewed!");
        }
    }

    private ResponseEntity<String> handleDefaultSubscriptionEvent(Event event) {
        return ResponseEntity.status(400).body("Unhandled subscription event type: " + event.eventType());
    }

    private ResponseEntity<String> handleCustomerCreated(Event event) {
        if (customerService.saveCustomer(event)) {
            return ResponseEntity.status(200).body("Customer saved successfully!");
        } else {
            return ResponseEntity.status(400).body("Customer already exists");
        }
    }

    private ResponseEntity<String> handleCustomerUpdated(Event event) {
        if (customerService.updateCustomer(event)) {
            return ResponseEntity.status(200).body("Customer updated successfully!");
        } else {
            return ResponseEntity.status(400).body("Customer not updated!");
        }
    }

    private ResponseEntity<String> handleCustomerDeleted(Event event) {
        if (customerService.deleteCustomer(event)) {
            return ResponseEntity.status(200).body("Customer deleted successfully!");
        } else {
            return ResponseEntity.status(400).body("Customer not found!");
        }
    }

    // private ResponseEntity<String> handleCustomerMigrated(Event event) {
    //     if (customerService.migrateCustomer(event)) {
    //         return ResponseEntity.status(200).body("Customer migrated successfully!");
    //     } else {
    //         return ResponseEntity.status(400).body("Customer migration failed!");
    //     }
    // }

    // private ResponseEntity<String> handleCustomerBillingAddressUpdated(Event event) {
    //     if (customerService.updateBillingAddress(event)) {
    //         return ResponseEntity.status(200).body("Customer billing address updated successfully!");
    //     } else {
    //         return ResponseEntity.status(400).body("Customer billing address not updated!");
    //     }
    // }

    private ResponseEntity<String> handleDefaultCustomerEvent(Event event) {
        return ResponseEntity.status(400).body("Unhandled customer event type: " + event.eventType());
    }

    @PostMapping("/invoices")
    @ResponseBody
    public ResponseEntity<String> handleInvoiceWebhook(@RequestBody Map<String, Object> e) {
        try {
            com.chargebee.models.Event event = com.chargebee.models.Event.retrieve(e.get("id").toString()).request().event();
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Invoice event already processed");
            }
            processedEvents.put(event.id(), true);
            String eventType = event.eventType().name();
            switch (eventType) {
                case "INVOICE_CREATED":
                case "INVOICE_GENERATED":
                case "INVOICE_UPDATED":
                case "INVOICE_PAID":
                case "INVOICE_VOIDED": {
                    invoiceService.saveOrUpdateInvoice(event);
                    customerService.saveCustomer(event); // idempotent
                    return ResponseEntity.status(200).body("Invoice event processed");
                }
                case "INVOICE_DELETED": {
                    invoiceService.deleteInvoice(event);
                    return ResponseEntity.status(200).body("Invoice deleted");
                }
                default: {
                    return ResponseEntity.status(400).body("Unhandled invoice event type: " + eventType);
                }
            }
        } catch (Exception ex) {
            logger.error("Error processing invoice webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error processing invoice webhook: " + ex.getMessage());
        }
    }

    @PostMapping("/payments")
    @ResponseBody
    public ResponseEntity<String> handlePaymentWebhook(@RequestBody Map<String, Object> e) {
        try {
            com.chargebee.models.Event event = com.chargebee.models.Event.retrieve(e.get("id").toString()).request().event();
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Payment event already processed");
            }
            processedEvents.put(event.id(), true);
            String eventType = event.eventType().name();
            switch (eventType) {
                case "PAYMENT_SUCCEEDED":
                case "PAYMENT_CREATED":
                case "PAYMENT_UPDATED": {
                    paymentService.saveOrUpdatePayment(event);
                    customerService.saveCustomer(event); // idempotent
                    return ResponseEntity.status(200).body("Payment event processed");
                }
                case "PAYMENT_DELETED": {
                    paymentService.deletePayment(event);
                    return ResponseEntity.status(200).body("Payment deleted");
                }
                default: {
                    return ResponseEntity.status(400).body("Unhandled payment event type: " + eventType);
                }
            }
        } catch (Exception ex) {
            logger.error("Error processing payment webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error processing payment webhook: " + ex.getMessage());
        }
    }

    @PostMapping("/credit-notes")
    @ResponseBody
    public ResponseEntity<String> handleCreditNoteWebhook(@RequestBody Map<String, Object> e) {
        try {
            com.chargebee.models.Event event = com.chargebee.models.Event.retrieve(e.get("id").toString()).request().event();
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Credit note event already processed");
            }
            processedEvents.put(event.id(), true);
            String eventType = event.eventType().name();
            switch (eventType) {
                case "CREDIT_NOTE_CREATED":
                case "CREDIT_NOTE_UPDATED":
                case "CREDIT_NOTE_VOIDED": {
                    creditNoteService.saveOrUpdateCreditNote(event);
                    customerService.saveCustomer(event); // idempotent
                    return ResponseEntity.status(200).body("Credit note event processed");
                }
                case "CREDIT_NOTE_DELETED": {
                    creditNoteService.deleteCreditNote(event);
                    return ResponseEntity.status(200).body("Credit note deleted");
                }
                default: {
                    return ResponseEntity.status(400).body("Unhandled credit note event type: " + eventType);
                }
            }
        } catch (Exception ex) {
            logger.error("Error processing credit note webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error processing credit note webhook: " + ex.getMessage());
        }
    }

    @PostMapping("/orders")
    @ResponseBody
    public ResponseEntity<String> handleOrderWebhook(@RequestBody Map<String, Object> e) {
        try {
            com.chargebee.models.Event event = com.chargebee.models.Event.retrieve(e.get("id").toString()).request().event();
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Order event already processed");
            }
            processedEvents.put(event.id(), true);
            String eventType = event.eventType().name();
            switch (eventType) {
                case "ORDER_CREATED":
                case "ORDER_UPDATED":
                case "ORDER_PROCESSED": {
                    orderService.saveOrUpdateOrder(event);
                    customerService.saveCustomer(event); // idempotent
                    return ResponseEntity.status(200).body("Order event processed");
                }
                case "ORDER_DELETED":
                case "ORDER_CANCELLED": {
                    orderService.deleteOrder(event);
                    return ResponseEntity.status(200).body("Order deleted");
                }
                default: {
                    return ResponseEntity.status(400).body("Unhandled order event type: " + eventType);
                }
            }
        } catch (Exception ex) {
            logger.error("Error processing order webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error processing order webhook: " + ex.getMessage());
        }
    }

    @PostMapping("/transactions")
    @ResponseBody
    public ResponseEntity<String> handleTransactionWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();

            // Idempotency check
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.ok("Event already processed");
            }

            // Process transaction event
            paymentService.processTransaction(event.content().transaction());
            processedEvents.put(event.id(), true);

            return ResponseEntity.ok("Transaction processed successfully");
        } catch (Exception ex) {
            logger.error("Error processing transaction webhook", ex);
            return ResponseEntity.status(500).body("Error processing transaction webhook");
        }
    }
}