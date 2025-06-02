package com.bookmyjuice.controllers;

import java.util.Map;

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
import com.chargebee.models.Event;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    @Autowired
    private SubscriptionService subscriptionService; // Assuming you have a SubscriptionService to handle business logic

    @Autowired
    private CustomerService customerService; // Assuming you have a CustomerService to handle business logic

    @Autowired
    private ItemService itemService; // Assuming you have an ItemService to handle business logic

    @PostMapping("/subscriptions")
    @ResponseBody
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> e) throws Exception {
        // Log the received event for debugging purposes
        Event event = Event.retrieve(e.get("id").toString()).request().event();
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
            case SUBSCRIPTION_CHANGED -> {
                // Handle subscription changed event if needed
                return ResponseEntity.status(200).body("Subscription changed event received");
            }
            default -> {
                return handleDefaultSubscriptionEvent(event);
            }
        }
    }

    @PostMapping("/customers")
    @ResponseBody
    public ResponseEntity<String> handleCustomerWebhook(@RequestBody Map<String, Object> e) throws Exception {
        Event event = Event.retrieve(e.get("id").toString()).request().event();
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
            default -> {
                return handleDefaultCustomerEvent(event);
            }
        }
    }

    @PostMapping("/items")
    @ResponseBody
    public ResponseEntity<?> handleItemsWebhook(@RequestBody Map<String, Object> e) throws Exception {
        Event event = Event.retrieve(e.get("id").toString()).request().event();
        switch (event.eventType()) {
            case ITEM_CREATED -> {
                return handleItemCreated(event);
            }
            case ITEM_UPDATED -> {
                return handleItemUpdated(event);
            }
            case ITEM_DELETED -> {
                return handleItemDeleted(event);
            }

            default -> {
                return ResponseEntity.status(400).body("Unhandled item event type: " + event.eventType());
            }
        }
    }
// Item webhook handlers --------------------------------------------

    /**
     * Handles the ITEM_CREATED event.
     *
     * @param event The Chargebee event containing item creation details.
     * @return A ResponseEntity indicating the result of the operation.
     */
    private ResponseEntity<?> handleItemCreated(Event event) {
        try {
            return itemService.saveItem(event);
        } catch (Exception err) {
            // Handle the exception (e.g., log it, return an error response, etc.)
            return ResponseEntity.status(500).body("Error processing webhook: " + err.getMessage());
        }
    }

    private ResponseEntity<?> handleItemUpdated(Event event) {
        try {
            return itemService.updateItem(event);

        } catch (Exception err) {
            // Handle the exception (e.g., log it, return an error response, etc.)
            return ResponseEntity.status(500).body("Error processing webhook: " + err.getMessage());
        }
    }

    private ResponseEntity<?> handleItemDeleted(Event event) {
        try {
            return itemService.deleteItem(event);
        } catch (Exception err) {
            // Handle the exception (e.g., log it, return an error response, etc.)
            return ResponseEntity.status(500).body("Error processing webhook: " + err.getMessage());
        }
    }
// Subscription webhook handlers --------------------------------------------

    private ResponseEntity<String> handleSubscriptionCreated(Event event) {
        try {
            if (subscriptionService.saveSubscriptions(event)) {
                return ResponseEntity.status(200).body("Webhook received and processed successfully");
            } else {
                return ResponseEntity.status(200).body("Subscription already exists");
            }
        } catch (Exception err) {
            // Handle the exception (e.g., log it, return an error response, etc.)
            return ResponseEntity.status(500).body("Error processing webhook: " + err.getMessage());
        }
    }

    private ResponseEntity<String> handleSubscriptionCancelled(Event event) {
        try {
            if (subscriptionService.updateSubscription(event)) {
                return ResponseEntity.status(200).body("Webhook received and processed successfully");
            } else {
                return ResponseEntity.status(400).body("Subscription not updated!");
            }
        } catch (Exception err) {
            return ResponseEntity.status(500).body("Error processing webhook: " + err.getMessage());
        }
    }

    private ResponseEntity<String> handleDefaultSubscriptionEvent(Event event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private ResponseEntity<String> handleSubscriptionPaused(Event event) {
        try {
            if (subscriptionService.updateSubscription(event)) {
                return ResponseEntity.status(200).body("Webhook received and processed successfully");
            } else {
                return ResponseEntity.status(400).body("Subscription not updated!");
            }
        } catch (Exception err) {
            return ResponseEntity.status(500).body("Error processing webhook: " + err.getMessage());
        }
    }

// Customer Webhhok Handlers --------------------------------------------
    private ResponseEntity<String> handleCustomerCreated(Event event) {
        try {
            if (customerService.saveCustomer(event)) {
                return ResponseEntity.status(200).body("Webhook received and processed successfully");
            } else {
                return ResponseEntity.status(200).body("Customer already exists");
            }
        } catch (Exception err) {
            // Handle the exception (e.g., log it, return an error response, etc.)
            return ResponseEntity.status(500).body("Error processing webhook: " + err.getMessage());
        }
    }

    private ResponseEntity<String> handleCustomerUpdated(Event event) {
        try {
            if (customerService.existsById(event.content().customer().id())) {
                customerService.updateCustomer(event);
                return ResponseEntity.status(200).body("Customer updated successfully!");
            } else {
                // Logic to handle customer update
                customerService.saveCustomer(event);
                return ResponseEntity.status(200).body("Customer didn't exist! New Customer Created!");
            }
        } catch (Exception err) {
            return ResponseEntity.status(500).body("Error processing customer updated event: " + err.getMessage());
        }
    }

    private ResponseEntity<String> handleCustomerDeleted(Event event) {
        try {
            // Logic to handle customer deletion
            customerService.deleteCustomer(event);
            return ResponseEntity.status(200).body("Customer deleted successfully!");
        } catch (Exception err) {
            return ResponseEntity.status(500).body("Error processing customer deleted event: " + err.getMessage());
        }
    }

    private ResponseEntity<String> handleDefaultCustomerEvent(Event event) {
        return ResponseEntity.status(400).body("Unhandled customer event type: " + event.eventType());
    }

}
