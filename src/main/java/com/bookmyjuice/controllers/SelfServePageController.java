package com.bookmyjuice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bookmyjuice.services.UserDetailsImpl;
import com.chargebee.Result;
import com.chargebee.models.PortalSession;

@Controller
@RequestMapping("api/test")
public class SelfServePageController {

    @GetMapping("/portal")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> postMethodName() {
        try {
            Result result = PortalSession.create()
                    
                    .customerId(getUserIdFromSecurityContext())
                    .request();
            PortalSession portalSession = result.portalSession();
            // System.err.println(portalSession.toString());
            return ResponseEntity.ok(portalSession.toJson());

        } catch (Exception e) {
            return ResponseEntity.ok(e.getMessage());
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
