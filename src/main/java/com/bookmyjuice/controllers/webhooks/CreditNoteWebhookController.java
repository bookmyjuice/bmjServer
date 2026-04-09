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

import com.bookmyjuice.services.CreditNoteService;
import com.bookmyjuice.services.CustomerService;
import com.bookmyjuice.services.IdempotencyService;

@RestController
@RequestMapping("/api/webhooks")
public class CreditNoteWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(CreditNoteWebhookController.class);

    @Autowired
    private CreditNoteService creditNoteService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private IdempotencyService idempotencyService;

    @PostMapping("/credit-notes")
    @ResponseBody
    public ResponseEntity<String> handleCreditNoteWebhook(@RequestBody Map<String, Object> e) {
        try {
            com.chargebee.models.Event event = com.chargebee.models.Event.retrieve(e.get("id").toString()).request().event();
            if (idempotencyService.checkAndMarkEvent(event.id())) {
                return ResponseEntity.status(200).body("Credit note event already processed");
            }
            
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
}