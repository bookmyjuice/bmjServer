package com.bookmyjuice.controllers;

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
import com.chargebee.models.Event;

@RestController
@RequestMapping("/api/webhooks/item-prices")    
public class ItemPriceWebhookController {
    @Autowired
    private ItemPriceService itemPriceService;

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
            switch (event.eventType()) {
                case ITEM_PRICE_CREATED -> {
                    itemPriceService.saveItemPrice(event);
                    return ResponseEntity.status(200).body("Item price created");
                }
                case ITEM_PRICE_UPDATED -> {
                    itemPriceService.updateItemPrice(event);
                    return ResponseEntity.status(200).body("Item price updated");
                }
                case ITEM_PRICE_DELETED -> {
                    itemPriceService.deleteItemPrice(event);
                    return ResponseEntity.status(200).body("Item price deleted");
                }
                default -> {
                    return ResponseEntity.status(400).body("Unhandled item price event type: " + event.eventType());
                }
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error processing item price webhook: " + ex.getMessage());
        }
    }
}
