// package online.bmj.www.controllers;

// import javax.validation.Valid;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.validation.annotation.Validated;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;

// import online.bmj.www.DTOs.SubscriptionRequest;
// import online.bmj.www.entities.SubscriptionEntity;
// import online.bmj.www.services.SubscriptionService;

// @RestController
// @RequestMapping("/subscriptions")
// @Validated
// public class SubscriptionController {

//     private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

//     @Autowired
//     private SubscriptionService subscriptionService;

//     @PostMapping("/create")
//     public ResponseEntity<SubscriptionEntity> createSubscription(
//             @Valid @RequestBody SubscriptionRequest request) {
//         logger.info("Creating subscription for customerId: {} and planId: {}",
//                     request.customerId(), request.planId());
//         SubscriptionEntity createdSubscription = subscriptionService.createSubscription(
//                 request.customerId(), request.planId());
//         return ResponseEntity.status(HttpStatus.CREATED).body(createdSubscription);
//     }

//     @PostMapping("/cancel")
//     public ResponseEntity<?> cancelSubscription(@RequestParam String subscriptionId) {
//         try {
//             var subscription = subscriptionService.cancelSubscription(subscriptionId);
//             return ResponseEntity.ok(subscription);
//         } catch (Exception e) {
//             return ResponseEntity.status(500).body(e.getMessage());
//         }
//     }

//     @PostMapping("/pause")
//     public ResponseEntity<?> pauseSubscription(@RequestParam String subscriptionId) {
//         try {
//             var subscription = subscriptionService.pauseSubscription(subscriptionId);
//             return ResponseEntity.ok(subscription);
//         } catch (Exception e) {
//             return ResponseEntity.status(500).body(e.getMessage());
//         }
//     }

//     @GetMapping("/{subscriptionId}")
//     public ResponseEntity<?> getSubscription(@PathVariable String subscriptionId) {
//         try {
//             var subscription = subscriptionService.getSubscription(subscriptionId);
//             return ResponseEntity.ok(subscription);
//         } catch (Exception e) {
//             return ResponseEntity.status(500).body(e.getMessage());
//         }
//     }

//     @PostMapping("/reactivate")
//     public ResponseEntity<?> reactivateSubscription(@RequestParam String subscriptionId) {
//         try {
//             var subscription = subscriptionService.reactivateSubscription(subscriptionId);
//             return ResponseEntity.ok(subscription);
//         } catch (Exception e) {
//             return ResponseEntity.status(500).body(e.getMessage());
//         }
//     }
// }