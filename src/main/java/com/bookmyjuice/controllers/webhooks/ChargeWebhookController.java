package com.bookmyjuice.controllers.webhooks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.services.ChargeService;
import com.chargebee.models.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/webhooks/charges")
public class ChargeWebhookController {
    @Autowired
    private ChargeService chargeService;

    private final Map<String, Boolean> processedEvents = new ConcurrentHashMap<>();

    @PostMapping
    @ResponseBody
    public ResponseEntity<String> handleChargeWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Event already processed");
            }
            processedEvents.put(event.id(), true);
            switch (event.eventType().name()) {
                case "CHARGE_CREATED" -> {
                    chargeService.saveCharge(event);
                    return ResponseEntity.status(200).body("Charge created");
                }
                case "CHARGE_UPDATED" -> {
                    chargeService.updateCharge(event);
                    return ResponseEntity.status(200).body("Charge updated");
                }
                case "CHARGE_DELETED" -> {
                    chargeService.deleteCharge(event);
                    return ResponseEntity.status(200).body("Charge deleted");
                }
                default -> {
                    return ResponseEntity.status(400).body("Unhandled charge event type: " + event.eventType());
                }
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error processing charge webhook: " + ex.getMessage());
        }
    }
}
