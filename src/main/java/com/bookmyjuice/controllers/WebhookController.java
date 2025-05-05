package com.bookmyjuice.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.services.SubscriptionService;
import com.chargebee.models.Event;


@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    @Autowired
    private  SubscriptionService subscriptionService; // Assuming you have a SubscriptionService to handle business logic
   
    // @PostMapping("/subscriptions")
    // public boolean postMethodName(@RequestBody Map<String,Object> entity) {
    //     return subscriptionService.saveSubscriptions(entity);
    // }
    @PostMapping("/subscriptions")
    @ResponseBody
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String,Object> e) throws Exception {
        // Log the received event for debugging purposes
        Event event = Event.retrieve(e.get("id").toString()).request().event();
        try {
        if (subscriptionService.saveSubscriptions(event)){
            return ResponseEntity.status(200).body("Webhook received and processed successfully");
        } else {
             return ResponseEntity.status(200).body("Subscription already exists");
        }}
        catch (Exception err) {
            // Handle the exception (e.g., log it, return an error response, etc.)
            return ResponseEntity.status(500).body("Error processing webhook: " + err.getMessage());
        }
    }        
}

