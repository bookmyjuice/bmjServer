package com.bookmyjuice.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OTP utility class for generating and verifying OTPs
 * BUG FIX: Added rate limiting (max 3 OTP requests per phone number per minute)
 */
@Component
public class OTPUtil {
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_MINUTES = 10; // OTP valid for 10 minutes
    private static final int MAX_OTP_REQUESTS_PER_MINUTE = 3;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60;
    private final Map<String, OTPData> otpStore = new HashMap<>();
    private final Map<String, RateLimitData> rateLimitStore = new HashMap<>();
    private final Random random = new Random();

    @Autowired
    private SmsService smsService;

    /**
     * Check if a phone number can send another OTP request (rate limiting).
     * BUG FIX: Prevents abuse by limiting to MAX_OTP_REQUESTS_PER_MINUTE requests per minute.
     */
    public boolean canSendOTP(String phoneNumber) {
        LocalDateTime now = LocalDateTime.now();
        RateLimitData rateData = rateLimitStore.get(phoneNumber);

        if (rateData == null) {
            // First request - allow
            rateLimitStore.put(phoneNumber, new RateLimitData(1, now));
            return true;
        }

        // Check if the time window has expired
        long secondsSinceFirstRequest = ChronoUnit.SECONDS.between(rateData.windowStart, now);
        if (secondsSinceFirstRequest > RATE_LIMIT_WINDOW_SECONDS) {
            // Reset window
            rateLimitStore.put(phoneNumber, new RateLimitData(1, now));
            return true;
        }

        // Check if within rate limit
        if (rateData.requestCount >= MAX_OTP_REQUESTS_PER_MINUTE) {
            return false;
        }

        // Increment counter
        rateData.requestCount++;
        return true;
    }

    /**
     * Generate a 6-digit OTP for a phone number
     */
    public String generateOTP(String phoneNumber) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        
        String generatedOTP = otp.toString();
        long expiryTime = System.currentTimeMillis() + (OTP_EXPIRY_MINUTES * 60 * 1000);
        
        otpStore.put(phoneNumber, new OTPData(generatedOTP, expiryTime, false));
        
        // Send OTP via Fast2SMS in production (falls back to console if API key not configured)
        boolean smsSent = smsService.sendOtpSms(phoneNumber, generatedOTP);
        if (smsSent) {
            System.out.println("✅ [SMS] OTP sent to " + phoneNumber + " via Fast2SMS");
        } else {
            System.out.println("⚠️ [DEV] OTP for " + phoneNumber + ": " + generatedOTP + " (SMS not sent - check Fast2SMS config)");
        }
        
        return generatedOTP;
    }

    /**
     * Verify OTP for a phone number
     */
    public boolean verifyOTP(String phoneNumber, String otp) {
        if (!otpStore.containsKey(phoneNumber)) {
            return false;
        }

        OTPData otpData = otpStore.get(phoneNumber);

        // Check if OTP has expired
        if (System.currentTimeMillis() > otpData.expiryTime) {
            otpStore.remove(phoneNumber);
            return false;
        }

        // Check if OTP has already been used
        if (otpData.isUsed) {
            return false;
        }

        // Verify OTP
        if (otpData.otp.equals(otp)) {
            // Mark OTP as used
            otpData.isUsed = true;
            return true;
        }

        return false;
    }

    /**
     * Clear OTP for a phone number after successful signup
     */
    public void clearOTP(String phoneNumber) {
        otpStore.remove(phoneNumber);
        rateLimitStore.remove(phoneNumber);
    }

    /**
     * Inner class to store rate limit data
     */
    private static class RateLimitData {
        int requestCount;
        LocalDateTime windowStart;

        RateLimitData(int requestCount, LocalDateTime windowStart) {
            this.requestCount = requestCount;
            this.windowStart = windowStart;
        }
    }

    /**
     * Inner class to store OTP data
     */
    private static class OTPData {
        String otp;
        long expiryTime;
        boolean isUsed;

        OTPData(String otp, long expiryTime, boolean isUsed) {
            this.otp = otp;
            this.expiryTime = expiryTime;
            this.isUsed = isUsed;
        }
    }
}
