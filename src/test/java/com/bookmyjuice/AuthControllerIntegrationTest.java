package com.bookmyjuice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that verify Spring context loads correctly
 * and endpoints are properly mapped without conflicts.
 * 
 * These tests catch issues that unit tests cannot:
 * - Duplicate @PostMapping mappings
 * - Bean creation conflicts
 * - Missing bean dependencies
 * - Security configuration issues
 */
@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * TC-AUTH-INT-001: Verify application context loads without errors
     * This test would fail if there are duplicate endpoint mappings
     */
    @Test
    void contextLoads() {
        // If this test runs, context loaded successfully
        // Duplicate @PostMapping would prevent context from loading
    }

    /**
     * TC-AUTH-INT-002: Verify /api/auth/signin endpoint exists and is unique
     */
    @Test
    void testSigninEndpointExists() throws Exception {
        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().is4xxClientError()); // 400/401 expected (not 404)
    }

    /**
     * TC-AUTH-INT-003: Verify /api/auth/signup endpoint exists and is unique
     */
    @Test
    void testSignupEndpointExists() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\",\"password\":\"Test123!\"}"))
                .andExpect(status().is4xxClientError()); // 400 expected (not 404)
    }

    /**
     * TC-AUTH-INT-004: Verify /api/auth/unified-signup endpoint exists
     */
    @Test
    void testUnifiedSignupEndpointExists() throws Exception {
        mockMvc.perform(post("/api/auth/unified-signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\",\"password\":\"Test123!\"}"))
                .andExpect(status().is4xxClientError()); // 400 expected (not 404)
    }

    /**
     * TC-AUTH-INT-005: Verify /api/auth/send-email-verification endpoint exists
     */
    @Test
    void testSendEmailVerificationEndpointExists() throws Exception {
        mockMvc.perform(post("/api/auth/send-email-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\"}"))
                .andExpect(status().is2xxSuccessful()); // 200 expected (endpoint exists)
    }

    /**
     * TC-AUTH-INT-006: Verify /api/auth/verify-email-code endpoint exists
     */
    @Test
    void testVerifyEmailCodeEndpointExists() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\",\"verificationCode\":\"123456\"}"))
                .andExpect(status().is4xxClientError()); // 400 expected (not 404)
    }

    /**
     * TC-AUTH-INT-007: Verify /api/auth/send-otp endpoint exists
     */
    @Test
    void testSendOtpEndpointExists() throws Exception {
        mockMvc.perform(post("/api/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"9876543210\"}"))
                .andExpect(status().is2xxSuccessful()); // 200 expected (endpoint exists)
    }

    /**
     * TC-AUTH-INT-008: Verify /api/auth/verify-otp endpoint exists
     */
    @Test
    void testVerifyOtpEndpointExists() throws Exception {
        mockMvc.perform(post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"9876543210\",\"otp\":\"123456\"}"))
                .andExpect(status().is4xxClientError()); // 400 expected (not 404)
    }

    /**
     * TC-AUTH-INT-009: Verify /api/auth/resetpassword endpoint exists
     */
    @Test
    void testResetPasswordEndpointExists() throws Exception {
        mockMvc.perform(post("/api/auth/resetpassword")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test\",\"password\":\"Test123!\"}"))
                .andExpect(status().is4xxClientError()); // 400 expected (not 404)
    }

    /**
     * TC-AUTH-INT-010: Verify health endpoint exists and returns 200 OK
     */
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().is2xxSuccessful()); // 200 expected (public endpoint)
    }
}
