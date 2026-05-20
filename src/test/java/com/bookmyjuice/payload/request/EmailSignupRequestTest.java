package com.bookmyjuice.payload.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailSignupRequestTest {

    @Test
    @DisplayName("Should set and get all properties")
    void testGettersAndSetters() {
        EmailSignupRequest request = new EmailSignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhone("9876543210");

        assertEquals("test@example.com", request.getEmail());
        assertEquals("SecurePass123!", request.getPassword());
        assertEquals("John", request.getFirstName());
        assertEquals("Doe", request.getLastName());
        assertEquals("9876543210", request.getPhone());
    }

    @Test
    @DisplayName("Should allow null optional fields")
    void testNullFields() {
        EmailSignupRequest request = new EmailSignupRequest();
        request.setEmail("test@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("John");

        assertNull(request.getLastName());
        assertNull(request.getPhone());
    }
}
