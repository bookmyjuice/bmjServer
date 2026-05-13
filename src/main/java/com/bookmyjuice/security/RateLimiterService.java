package com.bookmyjuice.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bookmyjuice.ChargeBeeConfig;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate limiting service using Bucket4j token bucket algorithm.
 * Prevents brute force attacks on authentication endpoints.
 */
@Slf4j
@Service
public class RateLimiterService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Bucket authBucket;
    private static final Logger logger = LoggerFactory.getLogger(ChargeBeeConfig.class);

    public RateLimiterService() {
        // Global rate limit: 100 requests per minute for all auth endpoints combined
        Bandwidth authLimit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        this.authBucket = Bucket4j.builder()
                .addLimit(authLimit)
                .build();
    }

    /**
     * Check if a specific IP address is rate limited for authentication attempts.
     * Limit: 10 attempts per 5 minutes per IP
     * 
     * @param ipAddress The IP address attempting authentication
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String ipAddress) {
        if (ipAddress == null) {
            return true; // null key not rate-limited
        }
        Bucket bucket = buckets.computeIfAbsent(ipAddress, k -> {

            Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(5)));
            return Bucket4j.builder()
                    .addLimit(limit)
                    .build();
        });

        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            logger.warn("Rate limit exceeded for IP: {}", ipAddress);
        }
        return allowed;
    }

    /**
     * Get remaining tokens for an IP address.
     * 
     * @param ipAddress The IP address
     * @return Number of remaining requests allowed
     */
    public long getRemainingTokens(String ipAddress) {
        Bucket bucket = buckets.get(ipAddress);
        if (bucket == null) {
            return 10; // Default limit
        }
        return bucket.getAvailableTokens();
    }

    /**
     * Check if global auth endpoint limit is exceeded.
     * 
     * @return true if within global limit, false if exceeded
     */
    public boolean isGlobalAuthLimitAllowed() {
        return authBucket.tryConsume(1);
    }

    /**
     * Reset rate limit for a specific IP (useful for admin operations).
     * 
     * @param ipAddress The IP address to reset
     */
    public void resetLimit(String ipAddress) {
        buckets.remove(ipAddress);
        logger.info("Rate limit reset for IP: {}", ipAddress);
    }

    /**
     * Clear all stored buckets (useful during testing or maintenance).
     */
    public void clearAllLimits() {
        buckets.clear();
        logger.info("All rate limits cleared");
    }
}
