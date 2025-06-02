package com.bookmyjuice.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookmyjuice.models.ItemEntity;
import com.bookmyjuice.models.SubscriptionEntity;
import com.bookmyjuice.services.SubscriptionService;
import com.bookmyjuice.services.UserDetailsImpl;
import com.chargebee.Result;
import com.chargebee.models.PricingPageSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("api/test")
public class PricingPageController {

    String pricingPageIdPremium = "01JR21FTRVHMMZ65H1M5HWPE92";
    String pricingPageIdSignature = "01JR21BSCS1H6KTK4366AWC5QD";
    String pricingPageIdDelight = "01JW9F69APDA3QNSW34MKGZNH2"; // Change this to PRICINGPAGEID_BASIC_STRING for basic plan
    String pricingPageId = "01JR21FTRVHMMZ65H1M5HWPE92";

    @Autowired
    SubscriptionService subscriptionService;

    @GetMapping("/generate_pricing_page_session_url")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> pricingPage() {
        // System.err.println(req.getSubscriptionId());
        String customerId = getUserIdFromSecurityContext();
        if (customerId == null) {
            return ResponseEntity.badRequest().body("Customer ID not found");
        } else {
            if (subscriptionService.existsByCustomerId(customerId)) {
                return generateExistingSubscriptionSessionURLs(subscriptionService.findByCustomerId(customerId));
            } else {
                return generateNewSubscriptionSessionURLs(customerId);
            }

        }
    }

    public String getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId().toString();
        }
        return null; // Or throw an exception
    }

    // private ResponseEntity<?> generateExistingSubscriptionSessions(List<SubscriptionEntity> subscriptions) {
    //     try {
    //         ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper for JSON conversion
    //         // Create a map to hold the URLs for different subscription types
    //         Map<String, Object> sessionUrls = new HashMap<>();
    //         for (SubscriptionEntity subscription : subscriptions) {
    //             for (ItemEntity item : subscription.getItems()) {
    //                 String itemType = item.getType();
    //                 String _pricingPageId = switch (itemType) {
    //                     case "premium" ->
    //                         pricingPageIdPremium;
    //                     case "signature" ->
    //                         pricingPageIdSignature;
    //                     case "delight" ->
    //                         pricingPageIdDelight;
    //                     default ->
    //                         pricingPageId; // Default to basic or other type
    //                 };

    //                 Result result = PricingPageSession.createForExistingSubscription()
    //                         .pricingPageId(_pricingPageId)
    //                         .subscriptionId(subscription.getId())
    //                         .request();
    //                 sessionUrls.put(itemType, objectMapper.convertValue(result.pricingPageSession(), new TypeReference<Map<String, Object>>() {}));
    //             }
    //             // Result result = PricingPageSession.createForExistingSubscription()
    //             //         .pricingPageId(pricingPageId)
    //             //         .subscriptionId(subscription..getId())
    //             //         .request();
    //             // sessionUrls.add(result.pricingPageSession().url());
    //         }
    //         return ResponseEntity.ok(sessionUrls);
    //     } catch (Exception e) {
    //         return ResponseEntity.badRequest().body(e.getMessage());
    //     }
    // }

    private ResponseEntity<?> generateExistingSubscriptionSessionURLs(List<SubscriptionEntity> subscriptions) {
    try {
        Map<String, Object> sessionUrls = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper for JSON conversion

        for (SubscriptionEntity subscription : subscriptions) {
            String subscriptionId = subscription.getId();
            if (subscription.getItems() != null && !subscription.getItems().isEmpty()) {
                for (ItemEntity item : subscription.getItems()) {
                    String itemFamilyId = item.getItemFamilyId();
                    switch (itemFamilyId) {
                        case "juices" -> {
                            PricingPageSession premiumSession = (PricingPageSession) generateExistingPremiumSubscriptionSession(subscriptionId).getBody();
                            if (premiumSession != null) {
                            sessionUrls.put("premium", objectMapper.readValue(premiumSession.toJson(), new TypeReference<Map<String, Object>>() {}));
                            }
                        }
                        case "signature-juices" -> {
                            PricingPageSession signatureSession = (PricingPageSession) generateExistingSignatureSubscriptionSession(subscriptionId).getBody();
                            if (signatureSession != null) {
                            sessionUrls.put("signature", objectMapper.readValue(signatureSession.toJson(), new TypeReference<Map<String, Object>>() {}));
                            }
                        }
                        case "delight-juices" -> {
                            PricingPageSession delightSession = (PricingPageSession) generateExistingDelightSubscriptionSession(subscriptionId).getBody();
                            if (delightSession != null) {
                            sessionUrls.put("delight", objectMapper.readValue(delightSession.toJson(), new TypeReference<Map<String, Object>>() {}));
                            }
                        }
                        
                        default -> {
                        }
                    }
                }
            }
        }

        // Check for missing options and generate new sessions if necessary
        if (!sessionUrls.containsKey("premium")) {
            PricingPageSession premiumSession = (PricingPageSession) generateNewPremiumSubscriptionPricingPage(subscriptions.get(0).getCustomer().getId()).getBody();
            if (premiumSession != null) {
                sessionUrls.put("premium", objectMapper.readValue(premiumSession.toJson(), new TypeReference<Map<String, Object>>() {}));
            }
        }
        if (!sessionUrls.containsKey("signature")) {
            PricingPageSession signatureSession = (PricingPageSession) generateNewSignatureSubscriptionPricingPage(subscriptions.getFirst().getCustomer().getId()).getBody();
            if (signatureSession != null) {
                sessionUrls.put("signature", objectMapper.readValue(signatureSession.toJson(), new TypeReference<Map<String, Object>>() {}));
            }
        }
        if (!sessionUrls.containsKey("delight")) {
            PricingPageSession delightSession = (PricingPageSession) generateNewDelightSubscriptionPricingPage(subscriptions.get(0).getCustomer().getId()).getBody();
            if (delightSession != null) {
                sessionUrls.put("delight", objectMapper.readValue(delightSession.toJson(), new TypeReference<Map<String, Object>>() {}));
            }
        }
        return ResponseEntity.ok(sessionUrls);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

    private ResponseEntity<?> generateExistingPremiumSubscriptionSession(String subscriptionId) {
        try {
            Result result = PricingPageSession.createForExistingSubscription()
                    .pricingPageId(pricingPageIdPremium)
                    .subscriptionId(subscriptionId)
                    // .customerId(req.getCustomerId())
                    .request();

            return ResponseEntity.ok(result.pricingPageSession());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ResponseEntity<?> generateExistingSignatureSubscriptionSession(String subscriptionId) {
        try {
            Result result = PricingPageSession.createForExistingSubscription()
                    .pricingPageId(pricingPageIdSignature)
                    .subscriptionId(subscriptionId)
                    // .customerId(req.getCustomerId())
                    .request();

            return ResponseEntity.ok(result.pricingPageSession());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/generate_existing_delight_subscription_session")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> generateExistingDelightSubscriptionSession(String subscriptionId) {
        try {
            Result result = PricingPageSession.createForExistingSubscription()
                    .pricingPageId(pricingPageIdDelight)
                    .subscriptionId(subscriptionId)
                    // .customerId(req.getCustomerId())
                    .request();

            return ResponseEntity.ok(result.pricingPageSession());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ResponseEntity<?> generateNewSubscriptionSessionURLs(String customerId) {
    try {
        ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper for JSON conversion
        Map<String, Object> sessionUrls = new HashMap<>();
        PricingPageSession premiumSession = (PricingPageSession) generateNewPremiumSubscriptionPricingPage(customerId).getBody();
        if (premiumSession != null) {
            sessionUrls.put("premium", objectMapper.readValue(premiumSession.toJson(), new TypeReference<Map<String, Object>>() {}));
        }
        PricingPageSession signatureSession = (PricingPageSession) generateNewSignatureSubscriptionPricingPage(customerId).getBody();
        if (signatureSession != null) {
            sessionUrls.put("signature", objectMapper.readValue(signatureSession.toJson(), new TypeReference<Map<String, Object>>() {}));
        }
        PricingPageSession delightSession = (PricingPageSession) generateNewDelightSubscriptionPricingPage(customerId).getBody();
        if (delightSession != null) {
            sessionUrls.put("delight", objectMapper.readValue(delightSession.toJson(), new TypeReference<Map<String, Object>>() {}));
        }
        // Object premiumPricingPageResponse = generateNewPremiumSubscriptionPricingPage(customerId).getBody();
        // sessionUrls.put("premium", objectMapper.convertValue(premiumPricingPageResponse, new TypeReference<Map<String, Object>>() {}));
        // Object signaturePricingPageResponse = generateNewSignatureSubscriptionPricingPage(customerId).getBody();
        // sessionUrls.put("signature", objectMapper.convertValue(signaturePricingPageResponse, new TypeReference<Map<String, Object>>() {}));
        // Object delightPricingPageResponse = generateNewDelightSubscriptionPricingPage(customerId).getBody();
        // sessionUrls.put("delight", objectMapper.convertValue(delightPricingPageResponse, new TypeReference<Map<String, Object>>() {}));
        return ResponseEntity.ok(sessionUrls);
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

    @GetMapping("/generate_new_premium_subscription_pricing_page")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> generateNewPremiumSubscriptionPricingPage(String customerId) {
        try {
            Result result = PricingPageSession.createForNewSubscription()
                    .pricingPageId(pricingPageIdPremium)
                    // .subscriptionId(req.getSubscriptionId())
                    .customerId(customerId)
                    .request();
            return ResponseEntity.ok(result.pricingPageSession());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/generate_new_signature_subscription_pricing_page")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    private ResponseEntity<?> generateNewSignatureSubscriptionPricingPage(String customerId) {
        try {
            Result result = PricingPageSession.createForNewSubscription()
                    .pricingPageId(pricingPageIdSignature)
                    // .subscriptionId(req.getSubscriptionId())
                    .customerId(customerId)
                    .request();
            return ResponseEntity.ok(result.pricingPageSession());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/generate_new_delight_subscription_pricing_page")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    private ResponseEntity<?> generateNewDelightSubscriptionPricingPage(String customerId) {
        try {
            Result result = PricingPageSession.createForNewSubscription()
                    .pricingPageId(pricingPageIdDelight)
                    // .subscriptionId(req.getSubscriptionId())
                    .customerId(customerId)
                    .request();
            return ResponseEntity.ok(result.pricingPageSession());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
