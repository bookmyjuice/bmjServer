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

import com.bookmyjuice.services.AddonService;
import com.chargebee.models.Event;

@RestController
@RequestMapping("/api/webhooks/addons")
public class AddonWebhookController {
    @Autowired
    private AddonService addonService;

    private final Map<String, Boolean> processedEvents = new ConcurrentHashMap<>();

    @PostMapping
    @ResponseBody
    public ResponseEntity<String> handleAddonWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Event already processed");
            }
            processedEvents.put(event.id(), true);
            switch (event.eventType()) {
                case ADDON_CREATED -> {
                    addonService.saveAddon(event);
                    return ResponseEntity.status(200).body("Addon created");
                }
                case ADDON_UPDATED -> {
                    addonService.updateAddon(event);
                    return ResponseEntity.status(200).body("Addon updated");
                }
                case ADDON_DELETED -> {
                    addonService.deleteAddon(event);
                    return ResponseEntity.status(200).body("Addon deleted");
                }
                default -> {
                    return ResponseEntity.status(400).body("Unhandled addon event type: " + event.eventType());
                }
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error processing addon webhook: " + ex.getMessage());
        }
    }
}
