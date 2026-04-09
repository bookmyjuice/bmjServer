package com.bookmyjuice.controllers.webhooks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.services.ItemPriceService;
import com.bookmyjuice.services.WebhookEventProcessor;
import com.chargebee.models.Event;

@RestController
@RequestMapping("/api/webhooks/item-prices")    
public class ItemPriceWebhookController {
    @Autowired
    private ItemPriceService itemPriceService;
    
    @Autowired
    private WebhookEventProcessor webhookEventProcessor;

    private final Map<String, Boolean> processedEvents = new ConcurrentHashMap<>();

    @PostMapping
    @ResponseBody
    public ResponseEntity<String> handleItemPriceWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Event already processed");
            }
            processedEvents.put(event.id(), true);
            
            // Use WebhookEventProcessor for comprehensive handling including parent Item verification
            ResponseEntity<?> result = webhookEventProcessor.processItemPriceEvent(event);
            
            // Convert the ResponseEntity<?> to ResponseEntity<String>
            if (result.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(result.getStatusCode()).body(result.getBody().toString());
            } else {
                return ResponseEntity.status(result.getStatusCode()).body(result.getBody().toString());
            }
            
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error processing item price webhook: " + ex.getMessage());
        }
    }
}
