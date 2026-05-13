package com.bookmyjuice.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Unit tests for OTPUtil
 *
 * TC-AUTH-015: OTP generation
 * TC-AUTH-016: OTP verification — valid OTP passes
 * TC-AUTH-017: OTP verification — wrong OTP fails
 * TC-AUTH-018: OTP verification — unknown phone fails
 */
class OTPUtilTest {

    private OTPUtil otpUtil;

    @BeforeEach
    void setUp() {
        otpUtil = new OTPUtil();
    }

    @Test
    @DisplayName("TC-AUTH-015: Generate OTP returns 6-digit string")
    void testGenerateOTP() {
        String phone = "9876543210";
        String otp = otpUtil.generateOTP(phone);

        assertNotNull(otp, "OTP should not be null");
        assertEquals(6, otp.length(), "OTP should be 6 digits");
        assertTrue(otp.matches("\\d{6}"), "OTP should be numeric");
    }

    @Test
    @DisplayName("TC-AUTH-016: Verify OTP succeeds with valid OTP")
    void testVerifyOTP_Valid() {
        String phone = "9876543210";
        String otp = otpUtil.generateOTP(phone);

        boolean result = otpUtil.verifyOTP(phone, otp);
        assertTrue(result, "Valid OTP should be accepted");
    }

    @Test
    @DisplayName("TC-AUTH-017: Verify OTP fails with wrong OTP")
    void testVerifyOTP_WrongOTP() {
        String phone = "9876543210";
        otpUtil.generateOTP(phone);

        boolean result = otpUtil.verifyOTP(phone, "000000");
        assertFalse(result, "Wrong OTP should be rejected");
    }

    @Test
    @DisplayName("TC-AUTH-018: Verify OTP fails for unknown phone")
    void testVerifyOTP_UnknownPhone() {
        boolean result = otpUtil.verifyOTP("9999999999", "123456");
        assertFalse(result, "Unknown phone should return false");
    }

    @Test
    @DisplayName("Verify OTP fails when reusing used OTP")
    void testVerifyOTP_ReuseOTP() {
        String phone = "9876543210";
        String otp = otpUtil.generateOTP(phone);

        // First use should succeed
        assertTrue(otpUtil.verifyOTP(phone, otp), "First use should succeed");

        // Second use should fail (OTP marked as used)
        assertFalse(otpUtil.verifyOTP(phone, otp), "Reusing OTP should fail");
    }

    @Test
    @DisplayName("Clear OTP removes stored data")
    void testClearOTP() {
        String phone = "9876543210";
        otpUtil.generateOTP(phone);

        otpUtil.clearOTP(phone);
        boolean result = otpUtil.verifyOTP(phone, "000000");
        assertFalse(result, "After clear, OTP should not be verifiable");
    }

    @Test
    @DisplayName("Generate OTP for different phones produces different codes")
    void testGenerateOTP_DifferentPhones() {
        String otp1 = otpUtil.generateOTP("9876543210");
        String otp2 = otpUtil.generateOTP("9123456780");

        // Very unlikely they're equal
        assertNotEquals(otp1, otp2, "OTPs for different phones should differ");
    }

    @Test
    @DisplayName("Verify OTP fails for phone with no OTP generated")
    void testVerifyOTP_NoOTPGenerated() {
        assertFalse(otpUtil.verifyOTP("9876543210", "123456"),
                "Verification should fail for phone with no OTP generated");
    }
}
