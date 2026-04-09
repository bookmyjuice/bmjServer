package com.bookmyjuice.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Email verification code utility class
 * Note: In production, this should be replaced with actual email service integration
 */
@Component
public class EmailVerificationService {
    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRY_MINUTES = 10; // Code valid for 10 minutes
    private final Map<String, VerificationCodeData> codeStore = new HashMap<>();
    private final Random random = new Random();

    /**
     * Generate a 6-digit verification code for an email
     */
    public String generateVerificationCode(String email) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }

        String generatedCode = code.toString();
        long expiryTime = System.currentTimeMillis() + (CODE_EXPIRY_MINUTES * 60 * 1000);

        codeStore.put(email.toLowerCase().trim(), new VerificationCodeData(generatedCode, expiryTime, false));

        // TODO: In production, send code via actual email service
        System.out.println("⚠️ [DEV] Email verification code for " + email + ": " + generatedCode);

        return generatedCode;
    }

    /**
     * Verify email verification code
     */
    public boolean verifyCode(String email, String code) {
        String emailKey = email.toLowerCase().trim();
        
        if (!codeStore.containsKey(emailKey)) {
            return false;
        }

        VerificationCodeData codeData = codeStore.get(emailKey);

        // Check if code has expired
        if (System.currentTimeMillis() > codeData.expiryTime) {
            codeStore.remove(emailKey);
            return false;
        }

        // Check if code has already been used
        if (codeData.isUsed) {
            return false;
        }

        // Verify code
        if (codeData.code.equals(code)) {
            // Mark code as used
            codeData.isUsed = true;
            return true;
        }

        return false;
    }

    /**
     * Clear verification code after successful verification
     */
    public void clearCode(String email) {
        codeStore.remove(email.toLowerCase().trim());
    }

    /**
     * Inner class to store verification code data
     */
    private static class VerificationCodeData {
        String code;
        long expiryTime;
        boolean isUsed;

        VerificationCodeData(String code, long expiryTime, boolean isUsed) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.isUsed = isUsed;
        }
    }
}
