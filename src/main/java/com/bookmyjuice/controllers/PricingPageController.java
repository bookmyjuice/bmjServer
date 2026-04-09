package com.bookmyjuice.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookmyjuice.models.entities.ItemEntity;
import com.bookmyjuice.models.entities.ItemPriceEntity;
import com.bookmyjuice.models.entities.SubscriptionEntity;
import com.bookmyjuice.repository.ItemPriceRepository;
import com.bookmyjuice.repository.ItemRepository;
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

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    ItemPriceRepository itemPriceRepository;

    @GetMapping("/generate_pricing_page_session_url")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> pricingPage() {
        // System.err.println(req.getSubscriptionId());
        String customerId = getUserIdFromSecurityContext();
        if (customerId == null) {
            return ResponseEntity.badRequest().body("Customer ID not found");
        } else {
                return generateNewSubscriptionSessionURLs(customerId);
        }
    }

    @PostMapping("/generate_plan_change_session_url")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> planChangePricingPage(@RequestBody String subscriptionId) {
            return generateExistingSubscriptionSessionURLs(subscriptionId);
    }


    public String getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId().toString();
        }
        return null; // Or throw an exception
    }

    private ResponseEntity<?> generateExistingSubscriptionSessionURLs(String subscriptionId) {
    try {
        Map<String, Object> sessionUrls = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        
        Optional<SubscriptionEntity> s = subscriptionService.findBySubscription(subscriptionId);
        if (s.isEmpty()) {
            return ResponseEntity.badRequest().body("Subscription not found");
        }
        
        SubscriptionEntity subscription = s.get();
        if (subscription.getSubscriptionItems().isEmpty()) {
            return ResponseEntity.badRequest().body("Subscription has no items");
        }
        
        subscription.getSubscriptionItems().forEach(item -> {
            String itemPriceId = item.getItemPriceId();
            // Find ItemPrice first, then get the associated Item
            ItemPriceEntity itemPrice = itemPriceRepository.findById(itemPriceId).orElse(null);
            if (itemPrice != null && itemPrice.getItem() != null) {
                ItemEntity itemEntity = itemPrice.getItem();
                String itemFamilyId = itemEntity.getItemFamilyId();
                switch (itemFamilyId) {
                    case "Premium" -> {
                        try {
                            PricingPageSession premiumSession = (PricingPageSession) generateExistingPremiumSubscriptionSession(subscriptionId).getBody();
                            if (premiumSession != null) {
                                sessionUrls.put("premium", objectMapper.readValue(premiumSession.toJson(), new TypeReference<Map<String, Object>>() {}));
                            }
                        } catch (Exception e) {
                            // Handle exception
                        }
                    }
                    case "Signature" -> {
                        try {
                            PricingPageSession signatureSession = (PricingPageSession) generateExistingSignatureSubscriptionSession(subscriptionId).getBody();
                            if (signatureSession != null) {
                                sessionUrls.put("signature", objectMapper.readValue(signatureSession.toJson(), new TypeReference<Map<String, Object>>() {}));
                            }
                        } catch (Exception e) {
                            // Handle exception
                        }
                    }
                    case "Delight" -> {
                        try {
                            PricingPageSession delightSession = (PricingPageSession) generateExistingDelightSubscriptionSession(subscriptionId).getBody();
                            if (delightSession != null) {
                                sessionUrls.put("delight", objectMapper.readValue(delightSession.toJson(), new TypeReference<Map<String, Object>>() {}));
                            }
                        } catch (Exception e) {
                            // Handle exception
                        }
                    }
                }
            }
        });
        
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
