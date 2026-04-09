// package com.bookmyjuice.controllers.webhooks;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.ResponseBody;
// import org.springframework.web.bind.annotation.RestController;

// import com.bookmyjuice.services.CreditNoteService;
// import com.bookmyjuice.services.CustomerService;
// import com.bookmyjuice.services.InvoiceService;
// import com.bookmyjuice.services.OrderService;
// import com.bookmyjuice.services.PaymentService;
// import com.bookmyjuice.services.SubscriptionService;
// import com.bookmyjuice.services.WebhookEventProcessor;
// import com.chargebee.models.Event;


// @RestController
// @RequestMapping("/api/webhooks")
// public class WebhookController {

//     private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

//     @Autowired
//     private SubscriptionService subscriptionService;

//     @Autowired
//     private CustomerService customerService;



//     @Autowired
//     private InvoiceService invoiceService;
//     @Autowired
//     private PaymentService paymentService;
//     @Autowired
//     private CreditNoteService creditNoteService;
//     @Autowired
//     private OrderService orderService;
    
//     @Autowired
//     private WebhookEventProcessor webhookEventProcessor;

//     // Idempotency tracking
//     private final Map<String, Boolean> processedEvents = new ConcurrentHashMap<>();

//     @PostMapping("/subscriptions")
//     @ResponseBody
//     public ResponseEntity<String> handleSubscriptionWebhook(@RequestBody Map<String, Object> e) {
//         try {
//             Event event = Event.retrieve(e.get("id").toString()).request().event();

//             // Idempotency check
//             if (processedEvents.containsKey(event.id())) {
//                 if(subscriptionService.existsByCustomerId(event.content().subscription().customerId())){
//                     logger.info("Subscription event already processed for customer ID: {}", event.content().subscription().customerId());
//                     return ResponseEntity.status(200).body("Subscription-creation already processed");
//                 } else {
//                     logger.warn("Subscription event already processed but no subscription found for customer ID: {}", event.content().subscription().customerId());
//                     return ResponseEntity.status(200).body("Event already processed");
//                 }
//             }
//             processedEvents.put(event.id(), true);
            
//             logger.debug("Event type: {}", event.eventType().getClass().getName());
//             logger.debug("Event type value: {}", event.eventType());
            
//             // Use the WebhookEventProcessor for comprehensive handling
//             return webhookEventProcessor.processSubscriptionEvent(event);
            
//         } catch (Exception ex) {
//             logger.error("Error processing subscription webhook: {}", ex.getMessage(), ex);
//             return ResponseEntity.status(500).body("Error processing subscription webhook: " + ex.getMessage());
//         }
//     }

//     @PostMapping("/customers")
//     @ResponseBody
//     public ResponseEntity<String> handleCustomerWebhook(@RequestBody Map<String, Object> e) {
//         try {
//             Event event = Event.retrieve(e.get("id").toString()).request().event();

//             // Idempotency check
//             if (processedEvents.containsKey(event.id())) {
//                 return ResponseEntity.status(200).body("Event already processed");
//             }
//             processedEvents.put(event.id(), true);

//             // Use the WebhookEventProcessor for comprehensive handling
//             return webhookEventProcessor.processCustomerEvent(event);
            
//         } catch (Exception ex) {
//             logger.error("Error processing customer webhook: {}", ex.getMessage(), ex);
//             return ResponseEntity.status(500).body("Error processing customer webhook: " + ex.getMessage());
//         }
//     }





//     @PostMapping("/invoices")
//     @ResponseBody
//     public ResponseEntity<String> handleInvoiceWebhook(@RequestBody Map<String, Object> e) {
//         try {
//             Event event = Event.retrieve(e.get("id").toString()).request().event();
//             if (processedEvents.containsKey(event.id())) {
//                 return ResponseEntity.status(200).body("Invoice event already processed");
//             }
//             processedEvents.put(event.id(), true);
            
//             // Use the WebhookEventProcessor for comprehensive handling
//             return webhookEventProcessor.processInvoiceEvent(event);
            
//         } catch (Exception ex) {
//             logger.error("Error processing invoice webhook: {}", ex.getMessage(), ex);
//             return ResponseEntity.status(500).body("Error processing invoice webhook: " + ex.getMessage());
//         }
//     }

//     @PostMapping("/payments")
//     @ResponseBody
//     public ResponseEntity<String> handlePaymentWebhook(@RequestBody Map<String, Object> e) {
//         try {
//             Event event = Event.retrieve(e.get("id").toString()).request().event();
//             if (processedEvents.containsKey(event.id())) {
//                 return ResponseEntity.status(200).body("Payment event already processed");
//             }
//             processedEvents.put(event.id(), true);
            
//             // Use the WebhookEventProcessor for comprehensive handling
//             return webhookEventProcessor.processPaymentEvent(event);
            
//         } catch (Exception ex) {
//             logger.error("Error processing payment webhook: {}", ex.getMessage(), ex);
//             return ResponseEntity.status(500).body("Error processing payment webhook: " + ex.getMessage());
//         }
//     }

//     @PostMapping("/credit-notes")
//     @ResponseBody
//     public ResponseEntity<String> handleCreditNoteWebhook(@RequestBody Map<String, Object> e) {
//         try {
//             com.chargebee.models.Event event = com.chargebee.models.Event.retrieve(e.get("id").toString()).request().event();
//             if (processedEvents.containsKey(event.id())) {
//                 return ResponseEntity.status(200).body("Credit note event already processed");
//             }
//             processedEvents.put(event.id(), true);
//             String eventType = event.eventType().name();
//             switch (eventType) {
//                 case "CREDIT_NOTE_CREATED":
//                 case "CREDIT_NOTE_UPDATED":
//                 case "CREDIT_NOTE_VOIDED": {
//                     creditNoteService.saveOrUpdateCreditNote(event);
//                     customerService.saveCustomer(event); // idempotent
//                     return ResponseEntity.status(200).body("Credit note event processed");
//                 }
//                 case "CREDIT_NOTE_DELETED": {
//                     creditNoteService.deleteCreditNote(event);
//                     return ResponseEntity.status(200).body("Credit note deleted");
//                 }
//                 default: {
//                     return ResponseEntity.status(400).body("Unhandled credit note event type: " + eventType);
//                 }
//             }
//         } catch (Exception ex) {
//             logger.error("Error processing credit note webhook: {}", ex.getMessage(), ex);
//             return ResponseEntity.status(500).body("Error processing credit note webhook: " + ex.getMessage());
//         }
//     }

//     @PostMapping("/orders")
//     @ResponseBody
//     public ResponseEntity<String> handleOrderWebhook(@RequestBody Map<String, Object> e) {
//         try {
//             com.chargebee.models.Event event = com.chargebee.models.Event.retrieve(e.get("id").toString()).request().event();
//             if (processedEvents.containsKey(event.id())) {
//                 return ResponseEntity.status(200).body("Order event already processed");
//             }
//             processedEvents.put(event.id(), true);
//             String eventType = event.eventType().name();
//             switch (eventType) {
//                 case "ORDER_CREATED":
//                 case "ORDER_UPDATED":
//                 case "ORDER_PROCESSED": {
//                     orderService.saveOrUpdateOrder(event);
//                     customerService.saveCustomer(event); // idempotent
//                     return ResponseEntity.status(200).body("Order event processed");
//                 }
//                 case "ORDER_DELETED":
//                 case "ORDER_CANCELLED": {
//                     orderService.deleteOrder(event);
//                     return ResponseEntity.status(200).body("Order deleted");
//                 }
//                 default: {
//                     return ResponseEntity.status(400).body("Unhandled order event type: " + eventType);
//                 }
//             }
//         } catch (Exception ex) {
//             logger.error("Error processing order webhook: {}", ex.getMessage(), ex);
//             return ResponseEntity.status(500).body("Error processing order webhook: " + ex.getMessage());
//         }
//     }

//     @PostMapping("/transactions")
//     @ResponseBody
//     public ResponseEntity<String> handleTransactionWebhook(@RequestBody Map<String, Object> e) {
//         try {
//             Event event = Event.retrieve(e.get("id").toString()).request().event();

//             // Idempotency check
//             if (processedEvents.containsKey(event.id())) {
//                 return ResponseEntity.ok("Event already processed");
//             }

//             // Process transaction event
//             paymentService.processTransaction(event.content().transaction());
//             processedEvents.put(event.id(), true);

//             return ResponseEntity.ok("Transaction processed successfully");
//         } catch (Exception ex) {
//             logger.error("Error processing transaction webhook", ex);
//             return ResponseEntity.status(500).body("Error processing transaction webhook");
//         }
//     }
// }