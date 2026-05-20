package com.bookmyjuice.payload.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageResponseTest {

    @Test
    @DisplayName("Should create MessageResponse with message")
    void testConstructor() {
        MessageResponse response = new MessageResponse("Test message");
        assertEquals("Test message", response.getMessage());
    }

    @Test
    @DisplayName("Should update message via setter")
    void testSetter() {
        MessageResponse response = new MessageResponse("Initial");
        response.setMessage("Updated");
        assertEquals("Updated", response.getMessage());
    }
}
