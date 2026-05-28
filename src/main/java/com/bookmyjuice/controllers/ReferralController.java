package com.bookmyjuice.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.services.ReferralService;

@RestController
@RequestMapping("/api/referral")
public class ReferralController {

  @Autowired
  private ReferralService referralService;

  @GetMapping("/code")
  public ResponseEntity<Map<String, Object>> getReferralCode() {
    // In production, extract userId from JWT SecurityContext
    String mockUserId = "user_test_001";
    Map<String, Object> info = referralService.getReferralInfo(mockUserId);
    return ResponseEntity.ok(info);
  }

  @PostMapping("/apply")
  public ResponseEntity<Map<String, Object>> applyReferralCode(
      @RequestBody Map<String, String> body) {
    String code = body.getOrDefault("referralCode", "");
    // In production, extract currentUserId from JWT
    String currentUserId = "user_current";
    Map<String, Object> result = referralService.applyReferralCode(code, currentUserId);
    if ((boolean) result.getOrDefault("success", false)) {
      return ResponseEntity.ok(result);
    }
    return ResponseEntity.badRequest().body(result);
  }
}