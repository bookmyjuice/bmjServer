package com.bookmyjuice.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerifyOTPRequestTest {

    @Test
    @DisplayName("Should create via no-arg constructor and setters")
    void testNoArgConstructor() {
        VerifyOTPRequest request = new VerifyOTPRequest();
        request.setPhone("9876543210");
        request.setOtp("123456");
        assertEquals("9876543210", request.getPhone());
        assertEquals("123456", request.getOtp());
    }

    @Test
    @DisplayName("Should create via parameterized constructor")
    void testParameterizedConstructor() {
        VerifyOTPRequest request = new VerifyOTPRequest("9876543210", "123456");
        assertEquals("9876543210", request.getPhone());
        assertEquals("123456", request.getOtp());
    }
}
