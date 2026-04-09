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

import com.bookmyjuice.services.PlanService;
import com.chargebee.models.Event;

@RestController
@RequestMapping("/api/webhooks/plans")
public class PlanWebhookController {
    @Autowired
    private PlanService planService;

    private final Map<String, Boolean> processedEvents = new ConcurrentHashMap<>();

    @PostMapping
    @ResponseBody
    public ResponseEntity<String> handlePlanWebhook(@RequestBody Map<String, Object> e) {
        try {
            Event event = Event.retrieve(e.get("id").toString()).request().event();
            if (processedEvents.containsKey(event.id())) {
                return ResponseEntity.status(200).body("Event already processed");
            }
            processedEvents.put(event.id(), true);
            switch (event.eventType()) {
                case PLAN_CREATED -> {
                    planService.savePlan(event);
                    return ResponseEntity.status(200).body("Plan created");
                }
                case PLAN_UPDATED -> {
                    planService.updatePlan(event);
                    return ResponseEntity.status(200).body("Plan updated");
                }
                case PLAN_DELETED -> {
                    planService.deletePlan(event);
                    return ResponseEntity.status(200).body("Plan deleted");
                }
                default -> {
                    return ResponseEntity.status(400).body("Unhandled plan event type: " + event.eventType());
                }
            }
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error processing plan webhook: " + ex.getMessage());
        }
    }
}
