package com.bookmyjuice.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * OTP utility class for generating and verifying OTPs
 * Note: In production, this should be replaced with actual SMS provider integration
 */
@Component
public class OTPUtil {
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_MINUTES = 10; // OTP valid for 10 minutes
    private final Map<String, OTPData> otpStore = new HashMap<>();
    private final Random random = new Random();

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
        
        // TODO: In production, send OTP via actual SMS provider
        System.out.println("⚠️ [DEV] OTP for " + phoneNumber + ": " + generatedOTP);
        
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
