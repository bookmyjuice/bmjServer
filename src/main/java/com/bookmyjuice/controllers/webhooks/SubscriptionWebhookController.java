package com.bookmyjuice.controllers.webhooks;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.services.IdempotencyService;
import com.bookmyjuice.services.SubscriptionService;
import com.bookmyjuice.services.WebhookEventProcessor;
import com.chargebee.models.Event;

@RestController
@RequestMapping("/api/webhooks")
public class SubscriptionWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionWebhookController.class);

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private WebhookEventProcessor webhookEventProcessor;

    @Autowired
    private IdempotencyService idempotencyService;

    @PostMapping("/subscriptions")
    @ResponseBody
    public ResponseEntity<String> handleSubscriptionWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();

            // Idempotency check
            if (idempotencyService.isEventProcessed(event.id())) {
                if(subscriptionService.existsByCustomerId(event.content().subscription().customerId())){
                    logger.info("Subscription event already processed for customer ID: {}", event.content().subscription().customerId());
                    return ResponseEntity.status(200).body("Subscription-creation already processed");
                } else {
                    logger.warn("Subscription event already processed but no subscription found for customer ID: {}", event.content().subscription().customerId());
                    return ResponseEntity.status(200).body("Event already processed");
                }
            }
            idempotencyService.markEventAsProcessed(event.id());
            
            logger.debug("Event type: {}", event.eventType().getClass().getName());
            logger.debug("Event type value: {}", event.eventType());
            
            // Use the WebhookEventProcessor for comprehensive handling
            return webhookEventProcessor.processSubscriptionEvent(event);
            
        } catch (Exception ex) {
            logger.error("Error processing subscription webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error processing subscription webhook: " + ex.getMessage());
        }
    }
}