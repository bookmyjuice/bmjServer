package com.bookmyjuice.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class ReferralService {

  // In-memory store: userId -> referralCode
  private final ConcurrentHashMap<String, String> referralCodes = new ConcurrentHashMap<>();
  // In-memory store: referralCode -> referrerUserId
  private final ConcurrentHashMap<String, String> codeToUser = new ConcurrentHashMap<>();
  // In-memory store: referrerUserId -> count of referred users
  private final ConcurrentHashMap<String, Integer> referralCounts = new ConcurrentHashMap<>();
  // In-memory store: referrerUserId -> total reward amount
  private final ConcurrentHashMap<String, Double> rewardAmounts = new ConcurrentHashMap<>();

  private final Random random = new Random();

  public ReferralService() {
    // Seed with test data
    referralCodes.put("user_test_001", "RAHUL23");
    codeToUser.put("RAHUL23", "user_test_001");
    referralCounts.put("user_test_001", 3);
    rewardAmounts.put("user_test_001", 150.0);
  }

  public String generateReferralCode(String userId, String name) {
    String prefix = name.length() >= 5
        ? name.substring(0, 5).toUpperCase()
        : name.toUpperCase();
    String suffix = userId.length() >= 3
        ? userId.substring(userId.length() - 3).toUpperCase()
        : userId.toUpperCase();
    String code = prefix + suffix;

    // Ensure uniqueness
    int attempt = 0;
    while (codeToUser.containsKey(code)) {
      attempt++;
      code = prefix + suffix + (random.nextInt(9) + 1);
      if (attempt > 10) {
        code = prefix + suffix + System.currentTimeMillis() % 1000;
      }
    }

    referralCodes.put(userId, code);
    codeToUser.put(code, userId);
    return code;
  }

  public Map<String, Object> getReferralInfo(String userId) {
    String code = referralCodes.getOrDefault(userId, generateReferralCode(userId, "User"));
    Map<String, Object> result = new HashMap<>();
    result.put("referralCode", code);
    result.put("referralCount", referralCounts.getOrDefault(userId, 0));
    result.put("totalRewardAmount", rewardAmounts.getOrDefault(userId, 0.0));
    return result;
  }

  public Map<String, Object> applyReferralCode(String code, String currentUserId) {
    Map<String, Object> result = new HashMap<>();

    if (code == null || code.trim().isEmpty()) {
      result.put("success", false);
      result.put("error", "Referral code cannot be empty");
      return result;
    }

    String referrerUserId = codeToUser.get(code.toUpperCase());
    if (referrerUserId == null) {
      result.put("success", false);
      result.put("error", "Invalid referral code");
      return result;
    }

    if (referrerUserId.equals(currentUserId)) {
      result.put("success", false);
      result.put("error", "Cannot use your own referral code");
      return result;
    }

    // Increment referrer's count
    referralCounts.merge(referrerUserId, 1, Integer::sum);
    // Award ₹50 to referrer
    rewardAmounts.merge(referrerUserId, 50.0, Double::sum);

    result.put("success", true);
    result.put("discountAmount", 50.0);
    return result;
  }
}