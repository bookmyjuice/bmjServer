package online.bmj.www.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.chargebee.Result;
import com.chargebee.models.PortalSession;
import com.chargebee.models.PricingPageSession;

import jakarta.validation.Valid;
import online.bmj.www.DTOs.pricingPageRequest;
import online.bmj.www.services.UserDetailsImpl;

@Controller
@RequestMapping("api/")
public class PricingPageController {

    String pricingPageId = "01JNNN99RMMJDPCVSAT6FGY0E6";

    @PostMapping("/portal")
    public ResponseEntity<?> postMethodName(@Valid @RequestBody pricingPageRequest data) {
        try {
            Result result = PortalSession.create()
                    // .redirectUrl("https://yourdomain.com/users/3490343")
                    .customerId(data.getCustomerId())
                    .request();
            PortalSession portalSession = result.portalSession();
            // System.err.println(portalSession.toString());
            return ResponseEntity.ok(portalSession.toJson());
         
        } catch (Exception e) {
            return ResponseEntity.ok(e.getMessage());
        }
    }

    @PostMapping("/generate_pricing_page_session_url")
    
    public ResponseEntity<?> pricingPage(@Valid @RequestBody pricingPageRequest req) {
        // System.err.println(req.getSubscriptionId());
        if (req.getIsExistingSubscription() == true) {
            return generateExistingSubscriptionSession(req);
        } else {
            return generateNewSubscriptionSession(req);
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

    private ResponseEntity<?> generateExistingSubscriptionSession(pricingPageRequest req) {
        try {
            Result result = PricingPageSession.createForExistingSubscription()
                    .pricingPageId(pricingPageId)
                    .subscriptionId(req.getSubscriptionId())
                    // .customerId(req.getCustomerId())
                    .request();
                    
            return ResponseEntity.ok(result.pricingPageSession().toJson());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ResponseEntity<?> generateNewSubscriptionSession(pricingPageRequest req) {
        try {
            Result result = PricingPageSession.createForNewSubscription()
                    .pricingPageId(pricingPageId)
                    .subscriptionId(req.getSubscriptionId())
                    .customerId(getUserIdFromSecurityContext())
                    .request();
            return ResponseEntity.ok(result.pricingPageSession().toJson());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
