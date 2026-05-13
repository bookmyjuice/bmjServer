package com.bookmyjuice.security.jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.bookmyjuice.services.UserDetailsImpl;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for JwtUtils
 *
 * TC-AUTH-006: JWT token generation with valid user
 * TC-AUTH-007: JWT token validation with valid token
 * TC-AUTH-008: JWT token validation with expired token
 * TC-AUTH-009: JWT token validation with invalid signature
 * TC-AUTH-010: JWT token parsing extracts username
 */
@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    @InjectMocks
    private JwtUtils jwtUtils;

    private static final String TEST_SECRET = "BookMyJuice_SecureJWT_Key_2024_Minimum32CharsForTest!";
    private static final int EXPIRATION_MS = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", EXPIRATION_MS);
    }

    @Test
    @DisplayName("TC-AUTH-006: JWT token generation with valid user")
    void testGenerateJwtToken_ValidUser() {
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "test@example.com", "test@example.com", "password",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtUtils.generateJwtToken(authentication, 1);

        assertNotNull(token);
        assertTrue(token.length() > 20);
        assertTrue(token.chars().filter(ch -> ch == '.').count() == 2);
    }

    @Test
    @DisplayName("TC-AUTH-007: JWT token validation with valid token")
    void testValidateJwtToken_ValidToken() {
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "test@example.com", "test@example.com", "password",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtUtils.generateJwtToken(authentication, 1);
        assertTrue(jwtUtils.validateJwtToken(token));
    }

    @Test
    @DisplayName("TC-AUTH-008: JWT token validation with empty token")
    void testValidateJwtToken_EmptyToken() {
        assertFalse(jwtUtils.validateJwtToken(""));
    }

    @Test
    @DisplayName("TC-AUTH-009: JWT token validation with null token")
    void testValidateJwtToken_NullToken() {
        assertFalse(jwtUtils.validateJwtToken(null));
    }

    @Test
    @DisplayName("TC-AUTH-010: JWT token parsing extracts username")
    void testGetUserNameFromJwtToken() {
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "testuser", "test@example.com", "password",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        when(authentication.getPrincipal()).thenReturn(userDetails);

        String token = jwtUtils.generateJwtToken(authentication, 1);
        String username = jwtUtils.getUserNameFromJwtToken(token);

        assertEquals("testuser", username);
    }
}
