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
import com.bookmyjuice.services.WebhookEventProcessor;
import com.chargebee.models.Event;

@RestController
@RequestMapping("/api/webhooks")
public class CustomerWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerWebhookController.class);

    @Autowired
    private WebhookEventProcessor webhookEventProcessor;

    @Autowired
    private IdempotencyService idempotencyService;

    @PostMapping("/customers")
    @ResponseBody
    public ResponseEntity<String> handleCustomerWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();

            // Idempotency check
            if (idempotencyService.checkAndMarkEvent(event.id())) {
                return ResponseEntity.status(200).body("Event already processed");
            }

            // Use the WebhookEventProcessor for comprehensive handling
            return webhookEventProcessor.processCustomerEvent(event);
            
        } catch (Exception ex) {
            logger.error("Error processing customer webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error processing customer webhook: " + ex.getMessage());
        }
    }
}