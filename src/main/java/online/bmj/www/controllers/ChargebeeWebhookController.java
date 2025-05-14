package online.bmj.www.controllers;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestHeader;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import online.bmj.www.services.SubscriptionService;
//
////ChargebeeWebhookController.java
//@RestController
//@RequestMapping("/webhooks/chargebee")
//public class ChargebeeWebhookController {
//	
//	@Autowired
//	private final SubscriptionService subscriptionService;
//
//	@PostMapping
//	public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
//			@RequestHeader("chargebee-webhook-signature") String signature) {
//		if (!verifySignature(payload, signature)) {
//			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//		}
//
//		WebhookEvent event = parsePayload(payload);
//		subscriptionService.processWebhookEvent(event);
//		return ResponseEntity.ok().build();
//	}
//
//	private boolean verifySignature(String payload, String signature) {
//		// Implement HMAC verification
//	}
//}