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
            String eventId = event.id();
            String eventType = event.eventType().name();

            // Start processing with database-level locking
            if (!idempotencyService.startEventProcessing(eventId, eventType)) {
                if (idempotencyService.isEventProcessed(eventId)) {
                    return ResponseEntity.status(200).body("Event already processed");
                } else {
                    return ResponseEntity.status(409).body("Event is currently being processed");
                }
            }

            try {
                // Use the WebhookEventProcessor for comprehensive handling
                ResponseEntity<String> result = webhookEventProcessor.processCustomerEvent(event);

                // Mark as completed if successful
                if (result.getStatusCode().is2xxSuccessful()) {
                    idempotencyService.markEventCompleted(eventId);
                } else {
                    idempotencyService.markEventFailed(eventId,
                            "Processing failed with status: " + result.getStatusCode());
                }

                return result;

            } catch (Exception processingEx) {
                logger.error("Error processing customer webhook: {}", processingEx.getMessage(), processingEx);
                idempotencyService.markEventFailed(eventId, processingEx.getMessage());
                return ResponseEntity.status(500)
                        .body("Error processing customer webhook: " + processingEx.getMessage());
            }

        } catch (Exception ex) {
            logger.error("Error retrieving customer webhook event: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error retrieving customer webhook event: " + ex.getMessage());
        }
    }
}