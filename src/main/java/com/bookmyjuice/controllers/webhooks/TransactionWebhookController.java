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
import com.bookmyjuice.services.PaymentService;
import com.chargebee.models.Event;

@RestController
@RequestMapping("/api/webhooks")
public class TransactionWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionWebhookController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private IdempotencyService idempotencyService;

    @PostMapping("/transactions")
    @ResponseBody
    public ResponseEntity<String> handleTransactionWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();

            // Idempotency check
            if (idempotencyService.checkAndMarkEvent(event.id())) {
                return ResponseEntity.ok("Event already processed");
            }

            // Process transaction event
            paymentService.processTransaction(event.content().transaction());

            return ResponseEntity.ok("Transaction processed successfully");
        } catch (Exception ex) {
            logger.error("Error processing transaction webhook", ex);
            return ResponseEntity.status(500).body("Error processing transaction webhook");
        }
    }
}