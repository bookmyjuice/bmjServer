package com.bookmyjuice.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookmyjuice.services.UserDetailsImpl;
import com.chargebee.Result;
import com.chargebee.models.HostedPage;

@Controller
@RequestMapping("api/test")
public class OneTimePageController {

    @GetMapping("/oneTimeCheckoutPageUrl")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> postMethodName() {
        try {
            Result result = HostedPage.checkoutOneTimeForItems()
                    .customerId(getUserIdFromSecurityContext())
                    .itemPriceItemPriceId(0, "ABC-INR")
                    // .(1,"cbdemo_one-time-setup-fee")
                    .itemPriceItemPriceId(1, "meteredTest-INR-Daily")
                    .request();
            HostedPage hostedPage = result.hostedPage();
            return ResponseEntity.ok(hostedPage.toJson());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/cartCheckout")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> cartCheckout() {
        try {
            Result result = HostedPage.checkoutOneTimeForItems()
                    .customerId(getUserIdFromSecurityContext())
                    .itemPriceItemPriceId(0, "ABC-INR")
                    .itemPriceItemPriceId(1, "delight300-INR-Weekly")
                    .itemPriceItemPriceId(2, "meteredTest-INR-Daily")
                    .request();
            HostedPage hostedPage = result.hostedPage();
            return ResponseEntity.ok(hostedPage.toJson());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
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
}
