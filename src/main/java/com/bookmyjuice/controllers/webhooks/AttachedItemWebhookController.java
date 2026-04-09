package com.bookmyjuice.controllers.webhooks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.services.AttachedItemService;
import com.chargebee.models.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/webhooks/attached-items")
public class AttachedItemWebhookController {
    @Autowired
    private AttachedItemService attachedItemService;

    private final Map<String, Boolean> processedEvents = new ConcurrentHashMap<>();

    @PostMapping
    @ResponseBody
    public ResponseEntity<String> handleAttachedItemWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Event already processed");
            }
            processedEvents.put(event.id(), true);
            switch (event.eventType()) {
                case ATTACHED_ITEM_CREATED -> {
                    attachedItemService.saveAttachedItem(event);
                    return ResponseEntity.status(200).body("Attached item created");
                }
                case ATTACHED_ITEM_UPDATED -> {
                    attachedItemService.updateAttachedItem(event);
                    return ResponseEntity.status(200).body("Attached item updated");
                }
                case ATTACHED_ITEM_DELETED -> {
                    attachedItemService.deleteAttachedItem(event);
                    return ResponseEntity.status(200).body("Attached item deleted");
                }
                default -> {
                    return ResponseEntity.status(400).body("Unhandled attached item event type: " + event.eventType());
                }
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error processing attached item webhook: " + ex.getMessage());
        }
    }
}
