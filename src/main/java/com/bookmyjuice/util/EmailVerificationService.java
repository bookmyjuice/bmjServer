package com.bookmyjuice.util;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Email verification code utility class.
 *
 * The code is generated and stored before delivery is attempted: a broken or
 * unconfigured SMTP relay must not brick the signup flow, so delivery failures
 * are logged (with the code, for local development) instead of thrown.
 */
@Component
public class EmailVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    @Autowired
    private JavaMailSender mailSender;

    private static final int CODE_LENGTH = 6;
    private static final long CODE_EXPIRY_MINUTES = 10; // Code valid for 10 minutes
    /** Minimum gap between two codes for the same address (anti email-bombing). */
    private static final long RESEND_COOLDOWN_SECONDS = 30;
    /** Wrong guesses allowed per issued code before it is discarded. */
    private static final int MAX_ATTEMPTS = 5;

    private final Map<String, VerificationCodeData> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSentAt = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * Generate a 6-digit verification code for an email and send it.
     *
     * @throws IllegalStateException when a code was issued less than
     *         {@value #RESEND_COOLDOWN_SECONDS} seconds ago
     */
    public String generateVerificationCode(String email) {
        String emailKey = normalise(email);
        long now = System.currentTimeMillis();

        Long previousSend = lastSentAt.get(emailKey);
        if (previousSend != null) {
            long retryAfterMs = (previousSend + RESEND_COOLDOWN_SECONDS * 1000) - now;
            if (retryAfterMs > 0) {
                throw new IllegalStateException("Please wait " + Math.max(1, retryAfterMs / 1000 + 1)
                        + " seconds before requesting another code");
            }
        }

        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }

        String generatedCode = code.toString();
        long expiryTime = now + (CODE_EXPIRY_MINUTES * 60 * 1000);

        codeStore.put(emailKey, new VerificationCodeData(generatedCode, expiryTime, false));
        lastSentAt.put(emailKey, now);

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("support@bookmyjuice.co.in");
        message.setTo(emailKey);
        message.setSubject("Your BookMyJuice Email Verification Code");
        message.setText("Your verification code is: " + generatedCode + "\nThis code will expire in "
                + CODE_EXPIRY_MINUTES + " minutes.");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            // Delivery is best-effort: the code is already stored, so signup can
            // still be completed when the relay is down (local dev / sandbox).
            logger.warn("Could not deliver email verification code to {}: {}", emailKey, e.getMessage());
        }

        // development log - remove in production
        System.out.println("⚠️ [DEV] Email verification code for " + emailKey + ": " + generatedCode);

        return generatedCode;
    }

    /**
     * Verify email verification code.
     *
     * The code is marked as used on success, so a second call with the same code
     * fails and callers must mint their own proof (JWT) right after a success.
     */
    public boolean verifyCode(String email, String code) {
        String emailKey = normalise(email);

        VerificationCodeData codeData = codeStore.get(emailKey);
        if (codeData == null) {
            return false;
        }

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

        codeData.attempts++;
        if (codeData.attempts >= MAX_ATTEMPTS) {
            codeStore.remove(emailKey);
            logger.warn("Discarded email verification code for {} after {} wrong attempts",
                    emailKey, codeData.attempts);
        }
        return false;
    }

    /**
     * Clear verification code after successful verification
     */
    public void clearCode(String email) {
        codeStore.remove(normalise(email));
    }

    private String normalise(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }

    /**
     * Inner class to store verification code data
     */
    private static class VerificationCodeData {
        String code;
        long expiryTime;
        boolean isUsed;
        int attempts;

        VerificationCodeData(String code, long expiryTime, boolean isUsed) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.isUsed = isUsed;
            this.attempts = 0;
        }
    }
}

