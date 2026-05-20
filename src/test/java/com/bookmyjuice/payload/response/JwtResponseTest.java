package com.bookmyjuice.payload.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JwtResponseTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create JwtResponse with 5-arg constructor (isNewUser defaults to false)")
        void testConstructor_5Args() {
            JwtResponse response = new JwtResponse("token123", 1L, "testuser", "test@example.com",
                    List.of("ROLE_USER"));

            assertEquals("token123", response.getAccessToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(1L, response.getId());
            assertEquals("testuser", response.getUsername());
            assertEquals("test@example.com", response.getEmail());
            assertEquals(List.of("ROLE_USER"), response.getRoles());
            assertFalse(response.isNewUser());
        }

        @Test
        @DisplayName("Should create JwtResponse with 6-arg constructor")
        void testConstructor_6Args() {
            JwtResponse response = new JwtResponse("token456", 2L, "admin", "admin@example.com",
                    List.of("ROLE_ADMIN"), true);

            assertEquals("token456", response.getAccessToken());
            assertEquals("Bearer", response.getTokenType());
            assertEquals(2L, response.getId());
            assertEquals("admin", response.getUsername());
            assertEquals("admin@example.com", response.getEmail());
            assertEquals(List.of("ROLE_ADMIN"), response.getRoles());
            assertTrue(response.isNewUser());
        }
    }

    @Nested
    @DisplayName("Setter tests")
    class SetterTests {

        @Test
        @DisplayName("Should update fields via setters")
        void testSetters() {
            JwtResponse response = new JwtResponse("tok", 1L, "u", "e@e.com", List.of("R"), false);

            response.setAccessToken("newToken");
            response.setTokenType("Custom");
            response.setId(99L);
            response.setUsername("newUser");
            response.setEmail("new@e.com");
            response.setNewUser(true);

            assertEquals("newToken", response.getAccessToken());
            assertEquals("Custom", response.getTokenType());
            assertEquals(99L, response.getId());
            assertEquals("newUser", response.getUsername());
            assertEquals("new@e.com", response.getEmail());
            assertEquals(List.of("R"), response.getRoles());
            assertTrue(response.isNewUser());
        }
    }
}
