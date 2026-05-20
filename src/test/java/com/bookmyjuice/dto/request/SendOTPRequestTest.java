package com.bookmyjuice.dto.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SendOTPRequestTest {

    @Test
    @DisplayName("Should create via no-arg constructor and setters")
    void testNoArgConstructor() {
        SendOTPRequest request = new SendOTPRequest();
        request.setPhone("9876543210");
        assertEquals("9876543210", request.getPhone());
    }

    @Test
    @DisplayName("Should create via parameterized constructor")
    void testParameterizedConstructor() {
        SendOTPRequest request = new SendOTPRequest("9876543210");
        assertEquals("9876543210", request.getPhone());
    }
}
