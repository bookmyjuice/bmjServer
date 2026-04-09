// package com.bookmyjuice.controllers.webhooks;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// /**
//  * Base WebhookController class.
//  * Individual webhook handlers have been separated into specific controller classes:
//  * - SubscriptionWebhookController
//  * - CustomerWebhookController
//  * - InvoiceWebhookController
//  * - PaymentWebhookController
//  * - CreditNoteWebhookController
//  * - OrderWebhookController
//  * - TransactionWebhookController
//  * 
//  * This class can be used for common webhook functionality or removed if not needed.
//  */
// @RestController
// @RequestMapping("/api/webhooks")
// public class WebhookControllerBackup {

//     private static final Logger logger = LoggerFactory.getLogger(WebhookControllerBackup.class);

//     // This class can be used for common webhook functionality
//     // or removed entirely if all handlers are separated
// }