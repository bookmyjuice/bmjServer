package com.bookmyjuice.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookmyjuice.services.ItemService;
import com.bookmyjuice.services.UserDetailsImpl;
import com.chargebee.Result;
import com.chargebee.models.HostedPage;

@Controller
@RequestMapping("api/test")
public class CheckoutController {

    @Autowired
    ItemService itemService;

    // public CheckoutController(ItemService itemService) {
    // this.itemService = itemService;
    // }

    @GetMapping("/oneTimeCheckoutPageUrl")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> postMethodName() {
        try {
            Result result = HostedPage.checkoutOneTimeForItems()
                    .customerId(getUserIdFromSecurityContext())
                    // .itemPriceItemPriceId(1, "ABC-INR")
                    // .itemPriceItemPriceId(0, "meteredTest-INR-Daily")
                    .itemPriceItemPriceId(0, "pineapple300-INR")
                    .request();
            HostedPage hostedPage = result.hostedPage();
            return ResponseEntity.ok(hostedPage.toJson());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/cartCheckout")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> cartCheckout(@RequestBody List<Map<String, Object>> cartItems) {
        try {
            HostedPage.CheckoutOneTimeForItemsRequest req = HostedPage.checkoutOneTimeForItems()
                    .customerId(getUserIdFromSecurityContext());
            for (int i = 0; i < cartItems.size(); i++) {
                Map<String, Object> item = cartItems.get(i);
                String itemPriceId = String.valueOf(item.get("itemPriceId"));
                int quantity = item.get("quantity") != null ? Integer.parseInt(item.get("quantity").toString()) : 1;
                req = req.itemPriceItemPriceId(i, itemPriceId).itemPriceQuantity(i, quantity);
            }
            Result result = req.request();
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
