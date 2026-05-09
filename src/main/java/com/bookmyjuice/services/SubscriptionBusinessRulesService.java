package com.bookmyjuice.services;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for handling business rules related to subscription operations.
 * Centralizes timezone-aware business logic for subscription management.
 */
@Service
public class SubscriptionBusinessRulesService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionBusinessRulesService.class);

    // Business constants
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int CUTOFF_HOUR = 21; // 9 PM IST

    /**
     * Check if subscription actions are allowed at the current time.
     * Actions are blocked after 9 PM IST to ensure they take effect the next day.
     *
     * @return true if actions are allowed, false if blocked due to cutoff
     */
    public boolean isSubscriptionActionAllowed() {
        ZonedDateTime istNow = ZonedDateTime.now(IST_ZONE);
        boolean allowed = istNow.getHour() < CUTOFF_HOUR;

        if (!allowed) {
            logger.info("Subscription action blocked due to 9 PM IST cutoff. Current IST time: {}", istNow);
        }

        return allowed;
    }

    /**
     * Get the current time in IST
     */
    public ZonedDateTime getCurrentISTTime() {
        return ZonedDateTime.now(IST_ZONE);
    }

    /**
     * Check if the current time is past the cutoff hour
     */
    public boolean isPastCutoff() {
        return ZonedDateTime.now(IST_ZONE).getHour() >= CUTOFF_HOUR;
    }

    /**
     * Get the cutoff hour (9 PM)
     */
    public int getCutoffHour() {
        return CUTOFF_HOUR;
    }

    /**
     * Get the IST timezone
     */
    public ZoneId getISTZone() {
        return IST_ZONE;
    }

    /**
     * Get a user-friendly message about the cutoff
     */
    public String getCutoffMessage() {
        return "Actions available until 9 PM IST. Changes will take effect next day.";
    }
}