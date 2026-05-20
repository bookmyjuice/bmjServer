package com.bookmyjuice.util;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SMS Service using Fast2SMS API for sending OTP messages.
 * 
 * Fast2SMS API: https://www.fast2sms.com/dev/bulkV2
 * POST endpoint with authorization header and query parameters:
 *   - authorization: API key
 *   - sender_id: Sender ID (e.g., "bookmyjuice")
 *   - message: SMS text
 *   - language: "english"
 *   - route: "q" (quick/transactional)
 *   - numbers: Comma-separated phone numbers
 * 
 * Environment variables:
 *   FAST2SMS_API_KEY - API key for Fast2SMS
 *   FAST2SMS_SENDER_ID - Sender ID (default: bookmyjuice)
 *   FAST2SMS_BASE_URL - API base URL (default: https://www.fast2sms.com/dev/bulkV2)
 */
@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    private final HttpClient httpClient;
    private final String apiKey;
    private final String senderId;
    private final String baseUrl;
    private final boolean enabled;

    public SmsService(
            @Value("${FAST2SMS_API_KEY:}") String apiKey,
            @Value("${FAST2SMS_SENDER_ID:bookmyjuice}") String senderId,
            @Value("${FAST2SMS_BASE_URL:https://www.fast2sms.com/dev/bulkV2}") String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKey = apiKey;
        this.senderId = senderId;
        this.baseUrl = baseUrl;
        this.enabled = apiKey != null && !apiKey.isEmpty() 
                && !apiKey.startsWith("replace") 
                && !apiKey.equals("your_fast2sms_api_key_here");
        
        if (this.enabled) {
            logger.info("SmsService initialized with Fast2SMS (sender: {}, API key length: {})", 
                    senderId, apiKey.length());
        } else {
            logger.warn("SmsService initialized in DEV mode - no valid Fast2SMS API key found. "
                    + "OTPs will be printed to console only.");
        }
    }

    /**
     * Send an OTP via SMS using Fast2SMS API.
     * 
     * @param phoneNumber The recipient's phone number (10 digits, without country code)
     * @param otp The 6-digit OTP to send
     * @return true if the SMS was sent successfully, false otherwise
     */
    public boolean sendOtpSms(String phoneNumber, String otp) {
        if (!enabled) {
            logger.warn("SMS sending disabled. Would have sent OTP {} to phone {}", otp, phoneNumber);
            return false;
        }

        try {
            String message = String.format(
                    "Your BookMyJuice OTP is %s. It is valid for 10 minutes. Do not share this OTP with anyone.",
                    otp);

            String body = buildRequestBody(phoneNumber, message);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("authorization", apiKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            logger.debug("Fast2SMS response {}: {}", response.statusCode(), response.body());

            if (response.statusCode() == 200) {
                // Parse response to verify success
                String respBody = response.body();
                if (respBody.contains("\"return\":true") || respBody.contains("\"return\": true")) {
                    logger.info("SMS sent successfully to {} via Fast2SMS", phoneNumber);
                    return true;
                } else {
                    logger.error("Fast2SMS API returned failure for phone {}: {}", phoneNumber, respBody);
                    return false;
                }
            } else {
                logger.error("Fast2SMS API returned HTTP {} for phone {}: {}", 
                        response.statusCode(), phoneNumber, response.body());
                return false;
            }
        } catch (java.net.ConnectException e) {
            logger.error("Fast2SMS connection failed (network issue): {}", e.getMessage());
            return false;
        } catch (java.net.http.HttpTimeoutException e) {
            logger.error("Fast2SMS request timed out for phone {}: {}", phoneNumber, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Failed to send SMS to phone {}: {}", phoneNumber, e.getMessage());
            return false;
        }
    }

    /**
     * Build the URL-encoded request body for Fast2SMS API.
     */
    private String buildRequestBody(String phoneNumber, String message) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("sender_id=").append(URLEncoder.encode(senderId, StandardCharsets.UTF_8));
            sb.append("&message=").append(URLEncoder.encode(message, StandardCharsets.UTF_8));
            sb.append("&language=").append(URLEncoder.encode("english", StandardCharsets.UTF_8));
            sb.append("&route=").append(URLEncoder.encode("q", StandardCharsets.UTF_8));
            sb.append("&numbers=").append(URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8));
            return sb.toString();
        } catch (Exception e) {
            logger.error("Error building Fast2SMS request body: {}", e.getMessage());
            return "";
        }
    }
}
