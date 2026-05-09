package com.bookmyjuice.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate limiting filter that protects authentication endpoints from brute force
 * attacks.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private static final String[] PROTECTED_PATHS = {
            "/api/auth/signin",
            "/api/auth/signup",
            "/api/auth/login-otp",
            "/api/auth/send-otp",
            "/api/auth/reset-password-mobile",
            "/api/auth/reset-password-email"
    };

    public RateLimitingFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (isProtectedPath(requestURI)) {
            String clientIp = getClientIp(request);

            // 1. Check Global Limit (Don't charge user bucket if server is overwhelmed)
            if (!rateLimiterService.isGlobalAuthLimitAllowed()) {
                sendErrorResponse(response, HttpStatus.SERVICE_UNAVAILABLE,
                        "Authentication service temporarily unavailable. Please try again later.");
                return;
            }

            // 2. Check IP-specific limit (Guard against null IP)
            if (clientIp == null || !rateLimiterService.isAllowed(clientIp)) {
                sendErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS,
                        "Too many authentication attempts. Please try again after 5 minutes.");
                return;
            }

            // 3. Success: Add headers
            long remainingTokens = rateLimiterService.getRemainingTokens(clientIp);
            response.addHeader("X-RateLimit-Remaining", String.valueOf(remainingTokens));
            response.addHeader("X-RateLimit-Limit", "10");
            response.addHeader("X-RateLimit-Reset", "300");
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"%s\"}", message));
    }

    private boolean isProtectedPath(String requestURI) {
        for (String path : PROTECTED_PATHS) {
            if (requestURI.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isProtectedPath(request.getRequestURI());
    }
}
