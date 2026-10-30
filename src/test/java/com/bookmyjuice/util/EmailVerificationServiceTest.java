package com.bookmyjuice.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Unit tests for EmailVerificationService
 *
 * TC-AUTH-011: Email verification code generation
 * TC-AUTH-012: Email verification code — valid code passes
 * TC-AUTH-013: Email verification code — wrong code fails
 * TC-AUTH-014: Email verification code — reusing used code fails
 */
class EmailVerificationServiceTest {

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService();
    }

    @Test
    @DisplayName("TC-AUTH-011: Generate verification code returns 6-digit string")
    void testGenerateVerificationCode() {
        String email = "test@example.com";
        String code = service.generateVerificationCode(email);

        assertNotNull(code, "Code should not be null");
        assertEquals(6, code.length(), "Code should be 6 digits");
        assertTrue(code.matches("\\d{6}"), "Code should be numeric");
    }

    @Test
    @DisplayName("TC-AUTH-012: Verify code succeeds with correct code")
    void testVerifyCode_Valid() {
        String email = "test@example.com";
        String code = service.generateVerificationCode(email);

        boolean result = service.verifyCode(email, code);
        assertTrue(result, "Valid code should be accepted");
    }

    @Test
    @DisplayName("TC-AUTH-013: Verify code fails with wrong code")
    void testVerifyCode_WrongCode() {
        String email = "test@example.com";
        service.generateVerificationCode(email);

        boolean result = service.verifyCode(email, "000000");
        assertFalse(result, "Wrong code should be rejected");
    }

    @Test
    @DisplayName("TC-AUTH-014: Verify code fails when reusing used code")
    void testVerifyCode_ReuseCode() {
        String email = "test@example.com";
        String code = service.generateVerificationCode(email);

        // First use should succeed
        assertTrue(service.verifyCode(email, code), "First use should succeed");

        // Second use should fail (code marked as used)
        assertFalse(service.verifyCode(email, code), "Reusing code should fail");
    }

    @Test
    @DisplayName("Verify code fails for unknown email")
    void testVerifyCode_UnknownEmail() {
        boolean result = service.verifyCode("unknown@example.com", "123456");
        assertFalse(result, "Unknown email should return false");
    }

    @Test
    @DisplayName("Clear code removes stored verification data")
    void testClearCode() {
        String email = "test@example.com";
        service.generateVerificationCode(email);

        service.clearCode(email);
        boolean result = service.verifyCode(email, "000000");
        assertFalse(result, "After clear, code should not be verifiable");
    }

    @Test
    @DisplayName("Email is normalized to lowercase")
    void testEmailNormalization() {
        String email = "Test.User@Example.Com";
        String code = service.generateVerificationCode(email);

        // Verify with different casing
        boolean result = service.verifyCode("test.user@example.com", code);
        assertTrue(result, "Email casing should be normalized");
    }

    @Test
    @DisplayName("A second code within the cooldown window is refused")
    void testResendCooldown() {
        String email = "cooldown@example.com";
        service.generateVerificationCode(email);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.generateVerificationCode(email));
        assertTrue(error.getMessage().contains("Please wait"), "Cooldown message should ask to wait");
    }

    @Test
    @DisplayName("A code is discarded after five wrong guesses")
    void testAttemptCap() {
        String email = "attempts@example.com";
        String code = service.generateVerificationCode(email);

        for (int i = 0; i < 5; i++) {
            assertFalse(service.verifyCode(email, "000000"), "Wrong guess should be rejected");
            // Guard against the generated code accidentally being 000000.
            if ("000000".equals(code)) {
                return;
            }
        }

        assertFalse(service.verifyCode(email, code), "Code should be discarded after the attempt cap");
    }

    @Test
    @DisplayName("Delivery failure does not throw and the code still verifies")
    void testDeliveryFailureIsNonFatal() {
        // mailSender is null in this unit test: sending throws, but the code is
        // stored first, so signup keeps working when the relay is unavailable.
        String email = "relay-down@example.com";
        String code = service.generateVerificationCode(email);

        assertTrue(service.verifyCode(email, code), "Stored code should verify despite delivery failure");
    }
}
