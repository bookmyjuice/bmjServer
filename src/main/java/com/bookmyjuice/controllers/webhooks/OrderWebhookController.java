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

import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.services.CustomerService;
import com.bookmyjuice.services.IdempotencyService;
import com.bookmyjuice.services.OrderService;

@RestController
@RequestMapping("/api/webhooks")
public class OrderWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(OrderWebhookController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/orders")
    @ResponseBody
    public ResponseEntity<String> handleOrderWebhook(@RequestBody Map<String, Object> e) {
        try {
            com.chargebee.models.Event event = com.chargebee.models.Event.retrieve(e.get("id").toString()).request()
                    .event();
            if (idempotencyService.checkAndMarkEvent(event.id())) {
                return ResponseEntity.status(200).body("Order event already processed");
            }

            String eventType = event.eventType().name();
            switch (eventType) {
                case "ORDER_CREATED":
                case "ORDER_UPDATED":
                case "ORDER_PROCESSED": {
                    orderService.saveOrUpdateOrder(event);
                    customerService.saveCustomer(event); // idempotent
                    return ResponseEntity.status(200).body("Order event processed");
                }
                case "ORDER_SHIPPED": {
                    orderService.saveOrUpdateOrder(event);
                    customerService.saveCustomer(event); // idempotent
                    return ResponseEntity.status(200).body("Order shipped event processed");
                }
                case "ORDER_DELIVERED": {
                    orderService.saveOrUpdateOrder(event);
                    customerService.saveCustomer(event); // idempotent
                    return ResponseEntity.status(200).body("Order delivered event processed");
                }
                case "ORDER_DELETED":
                case "ORDER_CANCELLED": {
                    orderService.deleteOrder(event);
                    return ResponseEntity.status(200).body("Order deleted");
                }
                default: {
                    return ResponseEntity.status(400).body("Unhandled order event type: " + eventType);
                }
            }
        } catch (Exception ex) {
            logger.error("Error processing order webhook: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body("Error processing order webhook: " + ex.getMessage());
        }
    }
}