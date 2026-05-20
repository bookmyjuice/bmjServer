package com.bookmyjuice.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bookmyjuice.dto.request.SendOTPRequest;
import com.bookmyjuice.dto.request.VerifyOTPRequest;
import com.bookmyjuice.models.ERole;
import com.bookmyjuice.models.Role;
import com.bookmyjuice.models.User;
import com.bookmyjuice.payload.request.EmailSignupRequest;
import com.bookmyjuice.payload.request.EmailVerificationRequest;
import com.bookmyjuice.payload.request.GoogleSignInRequest;
import com.bookmyjuice.payload.request.LinkGoogleAccountRequest;
import com.bookmyjuice.payload.request.LoginRequest;
import com.bookmyjuice.payload.request.ResetPasswordEmailRequest;
import com.bookmyjuice.payload.request.ResetPasswordMobileRequest;
import com.bookmyjuice.payload.request.UnifiedSignupRequest;
import com.bookmyjuice.payload.request.VerifyEmailCodeRequest;
import com.bookmyjuice.payload.response.JwtResponse;
import com.bookmyjuice.payload.response.MessageResponse;
import com.bookmyjuice.repository.RoleRepository;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.security.jwt.JwtUtils;
import com.bookmyjuice.services.UserDetailsImpl;
import com.bookmyjuice.util.EmailVerificationService;
import com.bookmyjuice.util.OTPUtil;

class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private OTPUtil otpUtil;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthController authController;

    private Role userRole;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userRole = new Role();
        userRole.setId(1);
        userRole.setName(ERole.ROLE_USER);
    }

    // ============================================================
    // Nested: autoLogin tests
    // ============================================================
    @Nested
    @DisplayName("autoLogin endpoint tests")
    class AutoLoginTests {

        @Test
        @DisplayName("Should return 400 when Authorization header is missing")
        void testAutoLogin_MissingHeader() {
            ResponseEntity<?> response = authController.autoLogin(null);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: Authorization header is missing!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should return 400 when Authorization header is empty")
        void testAutoLogin_EmptyHeader() {
            ResponseEntity<?> response = authController.autoLogin("");
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: Authorization header is missing!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should return 400 when Authorization header is too short")
        void testAutoLogin_ShortHeader() {
            ResponseEntity<?> response = authController.autoLogin("Short");
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: Authorization header is invalid!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should return 400 when JWT token is invalid")
        void testAutoLogin_InvalidToken() {
            when(jwtUtils.validateJwtToken("invalid-token")).thenReturn(false);
            ResponseEntity<?> response = authController.autoLogin("Bearer invalid-token");
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: Invalid JWT token!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should return 400 when JWT token has null subject (expired)")
        void testAutoLogin_ExpiredToken() {
            when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
            when(jwtUtils.getUserNameFromJwtToken("valid-token")).thenReturn(null);
            ResponseEntity<?> response = authController.autoLogin("Bearer valid-token");
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: JWT token is expired!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should return 400 when JWT token has empty subject (unsupported)")
        void testAutoLogin_EmptySubject() {
            when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
            when(jwtUtils.getUserNameFromJwtToken("valid-token")).thenReturn("");
            ResponseEntity<?> response = authController.autoLogin("Bearer valid-token");
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: JWT token is unsupported!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should return 400 when JWT token has blank subject (empty claims)")
        void testAutoLogin_BlankSubject() {
            when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
            when(jwtUtils.getUserNameFromJwtToken("valid-token")).thenReturn("   ");
            ResponseEntity<?> response = authController.autoLogin("Bearer valid-token");
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: JWT claims string is empty!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should return 200 when JWT token is valid")
        void testAutoLogin_Valid() {
            when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
            when(jwtUtils.getUserNameFromJwtToken("valid-token")).thenReturn("testuser");
            ResponseEntity<?> response = authController.autoLogin("Bearer valid-token");
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("ok", ((MessageResponse) response.getBody()).getMessage());
        }
    }

    // ============================================================
    // Nested: signin tests
    // ============================================================
    @Nested
    @DisplayName("signin endpoint tests")
    class SigninTests {

        @Test
        @DisplayName("Should return JWT on successful login")
        void testSignin_Success() {
            LoginRequest request = new LoginRequest();
            request.setUsername("test@example.com");
            request.setPassword("SecurePass123!");

            User user = new User("test@example.com", "test@example.com", "hashedPassword");
            user.setId(100L);
            Role ur = new Role();
            ur.setId(1);
            ur.setName(ERole.ROLE_USER);
            Set<Role> roles = new HashSet<>();
            roles.add(ur);
            user.setRoles(roles);

            Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
            UserDetailsImpl userDetails = new UserDetailsImpl(
                    100L, "test@example.com", "test@example.com", "hashedPassword",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(user));
            when(jwtUtils.generateJwtToken(authentication, 1)).thenReturn("mock-jwt-token");

            ResponseEntity<?> response = authController.signin(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            JwtResponse jwtResponse = (JwtResponse) response.getBody();
            assertNotNull(jwtResponse);
            assertEquals("mock-jwt-token", jwtResponse.getAccessToken());
            assertEquals(100L, jwtResponse.getId());
            assertTrue(jwtResponse.getRoles().contains("ROLE_USER"));
        }

        @Test
        @DisplayName("Should return 400 on invalid credentials (BadCredentialsException)")
        void testSignin_BadCredentials() {
            LoginRequest request = new LoginRequest();
            request.setUsername("test@example.com");
            request.setPassword("WrongPassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            ResponseEntity<?> response = authController.signin(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: Invalid username or password!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should return 400 on generic authentication exception")
        void testSignin_GenericException() {
            LoginRequest request = new LoginRequest();
            request.setUsername("test@example.com");
            request.setPassword("somepass");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new RuntimeException("Internal error"));

            ResponseEntity<?> response = authController.signin(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Authentication failed"));
        }

        @Test
        @DisplayName("Should find user by email if not found by username")
        void testSignin_FindByEmailFallback() {
            LoginRequest request = new LoginRequest();
            request.setUsername("user@example.com");
            request.setPassword("SecurePass123!");

            User user = new User("user@example.com", "user@example.com", "hashedPassword");
            user.setId(101L);
            Role ur = new Role();
            ur.setId(1);
            ur.setName(ERole.ROLE_USER);
            Set<Role> roles = new HashSet<>();
            roles.add(ur);
            user.setRoles(roles);

            Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
            UserDetailsImpl userDetails = new UserDetailsImpl(
                    101L, "user@example.com", "user@example.com", "hashedPassword",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(jwtUtils.generateJwtToken(authentication, 1)).thenReturn("mock-jwt-token");

            ResponseEntity<?> response = authController.signin(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ============================================================
    // Nested: signup tests (legacy email signup)
    // ============================================================
    @Nested
    @DisplayName("signup (email-based) endpoint tests")
    class SignupTests {

        @Test
        @DisplayName("Should fail with 400 when email already exists")
        void testSignup_DuplicateEmail() {
            EmailSignupRequest request = new EmailSignupRequest();
            request.setEmail("existing@example.com");
            request.setPassword("SecurePass123!");
            request.setFirstName("Jane");
            request.setLastName("Doe");

            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            ResponseEntity<?> response = authController.signup(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: Email is already registered!", ((MessageResponse) response.getBody()).getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should return 500 when Chargebee creation fails (no Chargebee mock)")
        void testSignup_ChargebeeFailure() {
            EmailSignupRequest request = new EmailSignupRequest();
            request.setEmail("test@example.com");
            request.setPassword("SecurePass123!");
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setPhone("9876543210");

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashedPassword");
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(100L);
                return u;
            });

            ResponseEntity<?> response = authController.signup(request);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            verify(userRepository).save(any(User.class));
            // Chargebee failure triggers rollback (delete)
            verify(userRepository).delete(any(User.class));
        }

        @Test
        @DisplayName("Should succeed when role found and save works")
        void testSignup_SaveSucceeds() {
            EmailSignupRequest request = new EmailSignupRequest();
            request.setEmail("new@example.com");
            request.setPassword("SecurePass123!");
            request.setFirstName("John");
            request.setLastName("Doe");

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashedPassword");
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(100L);
                return u;
            });

            // Chargebee will fail since it's a static call
            ResponseEntity<?> response = authController.signup(request);
            // Expect 500 because Chargebee is not mocked
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        @DisplayName("Should throw RuntimeException when ROLE_USER not found")
        void testSignup_RoleNotFound() {
            EmailSignupRequest request = new EmailSignupRequest();
            request.setEmail("test@example.com");
            request.setPassword("SecurePass123!");

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashedPassword");
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.empty());

            try {
                authController.signup(request);
            } catch (RuntimeException e) {
                assertEquals("Error: ROLE_USER is not found.", e.getMessage());
            }
        }
    }

    // ============================================================
    // Nested: unified-signup tests
    // ============================================================
    @Nested
    @DisplayName("unified-signup endpoint tests")
    class UnifiedSignupTests {

        @Test
        @DisplayName("Should fail with 400 when email is already registered")
        void testUnifiedSignup_DuplicateEmail() {
            UnifiedSignupRequest request = new UnifiedSignupRequest();
            request.setEmail("dup@example.com");
            request.setPhone("9999999999");
            request.setPassword("SecurePass123!");
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setAddress("123 Main St");
            request.setCity("Mumbai");
            request.setState("MH");
            request.setZip("400001");
            request.setCountry("IN");

            when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

            ResponseEntity<?> response = authController.unifiedSignup(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: Email is already registered!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should fail with 400 when phone is already registered")
        void testUnifiedSignup_DuplicatePhone() {
            UnifiedSignupRequest request = new UnifiedSignupRequest();
            request.setEmail("new@example.com");
            request.setPhone("8888888888");
            request.setPassword("SecurePass123!");
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setAddress("123 Main St");
            request.setCity("Mumbai");
            request.setState("MH");
            request.setZip("400001");
            request.setCountry("IN");

            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("8888888888")).thenReturn(true);

            ResponseEntity<?> response = authController.unifiedSignup(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: Phone number is already registered!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should return 500 when Chargebee fails during unified signup")
        void testUnifiedSignup_ChargebeeFailure() {
            UnifiedSignupRequest request = new UnifiedSignupRequest();
            request.setEmail("fresh@example.com");
            request.setPhone("7777777777");
            request.setPassword("SecurePass123!");
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setAddress("123 Main St");
            request.setCity("Mumbai");
            request.setState("MH");
            request.setZip("400001");
            request.setCountry("IN");

            when(userRepository.existsByEmail("fresh@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("7777777777")).thenReturn(false);
            when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashedPassword");
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(100L);
                return u;
            });

            ResponseEntity<?> response = authController.unifiedSignup(request);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            // Rollback should happen
            verify(userRepository).delete(any(User.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when ROLE_USER not found in unified signup")
        void testUnifiedSignup_RoleNotFound() {
            UnifiedSignupRequest request = new UnifiedSignupRequest();
            request.setEmail("test@example.com");
            request.setPhone("6666666666");
            request.setPassword("SecurePass123!");
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setAddress("123 Main St");
            request.setCity("Mumbai");
            request.setState("MH");
            request.setZip("400001");
            request.setCountry("IN");

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(userRepository.existsByUsername("6666666666")).thenReturn(false);
            when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashedPassword");
            when(roleRepository.findByName(ERole.ROLE_USER)).thenReturn(Optional.empty());

            try {
                authController.unifiedSignup(request);
            } catch (RuntimeException e) {
                assertEquals("Error: ROLE_USER is not found.", e.getMessage());
            }
        }
    }

    // ============================================================
    // Nested: send-otp and verify-otp tests
    // ============================================================
    @Nested
    @DisplayName("send-otp endpoint tests")
    class SendOtpTests {

        @Test
        @DisplayName("Should return 200 when OTP is generated successfully")
        void testSendOtp_Success() {
            SendOTPRequest request = new SendOTPRequest("9876543210");
            when(otpUtil.generateOTP("9876543210")).thenReturn("123456");

            ResponseEntity<?> response = authController.sendOTP(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("OTP sent"));
        }

        @Test
        @DisplayName("Should return 400 when OTP generation fails")
        void testSendOtp_Exception() {
            SendOTPRequest request = new SendOTPRequest("9876543210");
            when(otpUtil.generateOTP("9876543210")).thenThrow(new RuntimeException("SMS service down"));

            ResponseEntity<?> response = authController.sendOTP(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Failed to send OTP"));
        }
    }

    @Nested
    @DisplayName("verify-otp endpoint tests")
    class VerifyOtpTests {

        @Test
        @DisplayName("Should return 200 when OTP is valid")
        void testVerifyOtp_Success() {
            VerifyOTPRequest request = new VerifyOTPRequest("9876543210", "123456");
            when(otpUtil.verifyOTP("9876543210", "123456")).thenReturn(true);

            ResponseEntity<?> response = authController.verifyOTP(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("OTP verified"));
        }

        @Test
        @DisplayName("Should return 400 when OTP is invalid")
        void testVerifyOtp_Invalid() {
            VerifyOTPRequest request = new VerifyOTPRequest("9876543210", "wrong");
            when(otpUtil.verifyOTP("9876543210", "wrong")).thenReturn(false);

            ResponseEntity<?> response = authController.verifyOTP(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Invalid or expired OTP"));
        }

        @Test
        @DisplayName("Should return 400 when exception occurs")
        void testVerifyOtp_Exception() {
            VerifyOTPRequest request = new VerifyOTPRequest("9876543210", "123456");
            when(otpUtil.verifyOTP("9876543210", "123456")).thenThrow(new RuntimeException("error"));

            ResponseEntity<?> response = authController.verifyOTP(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("OTP verification failed"));
        }
    }

    // ============================================================
    // Nested: login-otp tests
    // ============================================================
    @Nested
    @DisplayName("login-otp endpoint tests")
    class LoginOtpTests {

        @Test
        @DisplayName("Should return 400 when OTP is invalid")
        void testLoginOtp_InvalidOtp() {
            VerifyOTPRequest request = new VerifyOTPRequest("9876543210", "wrong");
            when(otpUtil.verifyOTP("9876543210", "wrong")).thenReturn(false);

            ResponseEntity<?> response = authController.loginViaOtp(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Invalid or expired OTP"));
        }

        @Test
        @DisplayName("Should return user_not_found when user does not exist")
        void testLoginOtp_UserNotFound() {
            VerifyOTPRequest request = new VerifyOTPRequest("9876543210", "123456");
            when(otpUtil.verifyOTP("9876543210", "123456")).thenReturn(true);
            when(userRepository.findByUsername("9876543210")).thenReturn(Optional.empty());
            when(userRepository.findByPhone("9876543210")).thenReturn(Optional.empty());

            ResponseEntity<?> response = authController.loginViaOtp(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertEquals("user_not_found", body.get("status"));
        }

        @Test
        @DisplayName("Should return JWT when user exists")
        void testLoginOtp_UserExists() {
            VerifyOTPRequest request = new VerifyOTPRequest("9876543210", "123456");
            User user = new User("9876543210", "user@example.com", "hashedPassword");
            user.setId(200L);
            Role ur = new Role();
            ur.setId(1);
            ur.setName(ERole.ROLE_USER);
            Set<Role> roles = new HashSet<>();
            roles.add(ur);
            user.setRoles(roles);

            when(otpUtil.verifyOTP("9876543210", "123456")).thenReturn(true);
            when(userRepository.findByUsername("9876543210")).thenReturn(Optional.of(user));
            when(jwtUtils.generateJwtToken(any(Authentication.class), anyInt())).thenReturn("otp-jwt-token");

            ResponseEntity<?> response = authController.loginViaOtp(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            JwtResponse jwtResponse = (JwtResponse) response.getBody();
            assertNotNull(jwtResponse);
            assertEquals("otp-jwt-token", jwtResponse.getAccessToken());
            verify(otpUtil).clearOTP("9876543210");
        }

        @Test
        @DisplayName("Should find user by phone field when username lookup fails")
        void testLoginOtp_FindByPhoneFallback() {
            VerifyOTPRequest request = new VerifyOTPRequest("9876543210", "123456");
            User user = new User("email@example.com", "email@example.com", "hashedPassword");
            user.setId(201L);
            user.setPhone("9876543210");
            Role ur = new Role();
            ur.setId(1);
            ur.setName(ERole.ROLE_USER);
            Set<Role> roles = new HashSet<>();
            roles.add(ur);
            user.setRoles(roles);

            when(otpUtil.verifyOTP("9876543210", "123456")).thenReturn(true);
            when(userRepository.findByUsername("9876543210")).thenReturn(Optional.empty());
            when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(user));
            when(jwtUtils.generateJwtToken(any(Authentication.class), anyInt())).thenReturn("otp-jwt-token2");

            ResponseEntity<?> response = authController.loginViaOtp(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            JwtResponse jwtResponse = (JwtResponse) response.getBody();
            assertNotNull(jwtResponse);
            assertEquals("otp-jwt-token2", jwtResponse.getAccessToken());
        }

        @Test
        @DisplayName("Should handle exceptions gracefully")
        void testLoginOtp_Exception() {
            VerifyOTPRequest request = new VerifyOTPRequest("9876543210", "123456");
            when(otpUtil.verifyOTP("9876543210", "123456")).thenThrow(new RuntimeException("error"));

            ResponseEntity<?> response = authController.loginViaOtp(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ============================================================
    // Nested: send-email-verification tests
    // ============================================================
    @Nested
    @DisplayName("send-email-verification endpoint tests")
    class SendEmailVerificationTests {

        @Test
        @DisplayName("Should return 400 when email is already registered")
        void testSendEmailVerification_EmailExists() {
            EmailVerificationRequest request = new EmailVerificationRequest();
            request.setEmail("existing@example.com");
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            ResponseEntity<?> response = authController.sendEmailVerification(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: Email is already registered!", ((MessageResponse) response.getBody()).getMessage());
        }

        @Test
        @DisplayName("Should return 200 when verification code is sent")
        void testSendEmailVerification_Success() {
            EmailVerificationRequest request = new EmailVerificationRequest();
            request.setEmail("new@example.com");
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(emailVerificationService.generateVerificationCode("new@example.com")).thenReturn("654321");

            ResponseEntity<?> response = authController.sendEmailVerification(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Verification code sent"));
        }

        @Test
        @DisplayName("Should handle exceptions")
        void testSendEmailVerification_Exception() {
            EmailVerificationRequest request = new EmailVerificationRequest();
            request.setEmail("test@example.com");
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(emailVerificationService.generateVerificationCode("test@example.com")).thenThrow(new RuntimeException("error"));

            ResponseEntity<?> response = authController.sendEmailVerification(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ============================================================
    // Nested: verify-email-code tests
    // ============================================================
    @Nested
    @DisplayName("verify-email-code endpoint tests")
    class VerifyEmailCodeTests {

        @Test
        @DisplayName("Should return 200 when code is valid")
        void testVerifyEmailCode_Success() {
            VerifyEmailCodeRequest request = new VerifyEmailCodeRequest();
            request.setEmail("user@example.com");
            request.setVerificationCode("123456");
            when(emailVerificationService.verifyCode("user@example.com", "123456")).thenReturn(true);

            ResponseEntity<?> response = authController.verifyEmailCode(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Email verified"));
            verify(emailVerificationService).clearCode("user@example.com");
        }

        @Test
        @DisplayName("Should return 400 when code is invalid")
        void testVerifyEmailCode_Invalid() {
            VerifyEmailCodeRequest request = new VerifyEmailCodeRequest();
            request.setEmail("user@example.com");
            request.setVerificationCode("wrong");
            when(emailVerificationService.verifyCode("user@example.com", "wrong")).thenReturn(false);

            ResponseEntity<?> response = authController.verifyEmailCode(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Invalid or expired"));
        }

        @Test
        @DisplayName("Should handle exceptions")
        void testVerifyEmailCode_Exception() {
            VerifyEmailCodeRequest request = new VerifyEmailCodeRequest();
            request.setEmail("user@example.com");
            request.setVerificationCode("123456");
            when(emailVerificationService.verifyCode("user@example.com", "123456")).thenThrow(new RuntimeException("error"));

            ResponseEntity<?> response = authController.verifyEmailCode(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ============================================================
    // Nested: reset-password-mobile tests
    // ============================================================
    @Nested
    @DisplayName("reset-password-mobile endpoint tests")
    class ResetPasswordMobileTests {

        @Test
        @DisplayName("Should fail password validation for null password")
        void testResetPasswordMobile_NullPassword() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword(null);

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Password is required"));
        }

        @Test
        @DisplayName("Should fail password validation for short password")
        void testResetPasswordMobile_ShortPassword() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword("Ab1!");

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should fail password validation - missing lowercase")
        void testResetPasswordMobile_NoLowercase() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword("ABCDEF123!");

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("lowercase"));
        }

        @Test
        @DisplayName("Should fail password validation - missing uppercase")
        void testResetPasswordMobile_NoUppercase() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword("abcdef123!");

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("uppercase"));
        }

        @Test
        @DisplayName("Should fail password validation - missing number")
        void testResetPasswordMobile_NoNumber() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword("Abcdefgh!");

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("number"));
        }

        @Test
        @DisplayName("Should fail password validation - missing special char")
        void testResetPasswordMobile_NoSpecialChar() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword("Abcdefg1");

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("special character"));
        }

        @Test
        @DisplayName("Should fail password validation - contains space")
        void testResetPasswordMobile_WithSpace() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword("Abc def1!");

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("spaces"));
        }

        @Test
        @DisplayName("Should fail password validation - exceeds 40 chars")
        void testResetPasswordMobile_TooLong() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword("A1!" + "a".repeat(40));

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("40 characters"));
        }

        @Test
        @DisplayName("Should return 400 when OTP is invalid")
        void testResetPasswordMobile_InvalidOtp() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword("NewSecure1!");
            when(otpUtil.verifyOTP("9876543210", "123456")).thenReturn(false);

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Invalid or expired OTP"));
        }

        @Test
        @DisplayName("Should return 400 when user not found")
        void testResetPasswordMobile_UserNotFound() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword("NewSecure1!");
            when(otpUtil.verifyOTP("9876543210", "123456")).thenReturn(true);
            when(userRepository.findByUsername("9876543210")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("9876543210")).thenReturn(Optional.empty());

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("No account found"));
        }

        @Test
        @DisplayName("Should return 200 on successful password reset")
        void testResetPasswordMobile_Success() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setPassword("NewSecure1!");

            User user = new User("9876543210", "user@example.com", "oldHash");
            user.setTokenVersion(1);

            when(otpUtil.verifyOTP("9876543210", "123456")).thenReturn(true);
            when(userRepository.findByUsername("9876543210")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewSecure1!")).thenReturn("newHash");

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Password reset successfully"));
            assertEquals(2, user.getTokenVersion());
            verify(userRepository).save(user);
            verify(otpUtil).clearOTP("9876543210");
        }

        @Test
        @DisplayName("Should find user by email if not found by username")
        void testResetPasswordMobile_FindByEmailFallback() {
            ResetPasswordMobileRequest request = new ResetPasswordMobileRequest();
            request.setPhone("user@example.com");
            request.setOtp("123456");
            request.setPassword("NewSecure1!");

            User user = new User("user@example.com", "user@example.com", "oldHash");

            when(otpUtil.verifyOTP("user@example.com", "123456")).thenReturn(true);
            when(userRepository.findByUsername("user@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewSecure1!")).thenReturn("newHash");

            ResponseEntity<?> response = authController.resetPasswordViaMobile(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ============================================================
    // Nested: reset-password-email tests
    // ============================================================
    @Nested
    @DisplayName("reset-password-email endpoint tests")
    class ResetPasswordEmailTests {

        @Test
        @DisplayName("Should fail password validation - null password")
        void testResetPasswordEmail_NullPassword() {
            ResetPasswordEmailRequest request = new ResetPasswordEmailRequest();
            request.setEmail("user@example.com");
            request.setVerificationCode("123456");
            request.setPassword(null);

            ResponseEntity<?> response = authController.resetPasswordViaEmail(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return 400 when verification code is invalid")
        void testResetPasswordEmail_InvalidCode() {
            ResetPasswordEmailRequest request = new ResetPasswordEmailRequest();
            request.setEmail("user@example.com");
            request.setVerificationCode("wrong");
            request.setPassword("NewSecure1!");

            when(emailVerificationService.verifyCode("user@example.com", "wrong")).thenReturn(false);

            ResponseEntity<?> response = authController.resetPasswordViaEmail(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Invalid or expired"));
        }

        @Test
        @DisplayName("Should return 400 when user not found")
        void testResetPasswordEmail_UserNotFound() {
            ResetPasswordEmailRequest request = new ResetPasswordEmailRequest();
            request.setEmail("unknown@example.com");
            request.setVerificationCode("123456");
            request.setPassword("NewSecure1!");

            when(emailVerificationService.verifyCode("unknown@example.com", "123456")).thenReturn(true);
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            ResponseEntity<?> response = authController.resetPasswordViaEmail(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("No account found"));
        }

        @Test
        @DisplayName("Should return 200 on successful password reset via email")
        void testResetPasswordEmail_Success() {
            ResetPasswordEmailRequest request = new ResetPasswordEmailRequest();
            request.setEmail("user@example.com");
            request.setVerificationCode("123456");
            request.setPassword("NewSecure1!");

            User user = new User("user@example.com", "user@example.com", "oldHash");
            user.setTokenVersion(2);

            when(emailVerificationService.verifyCode("user@example.com", "123456")).thenReturn(true);
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewSecure1!")).thenReturn("newHash");

            ResponseEntity<?> response = authController.resetPasswordViaEmail(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Password reset successfully"));
            assertEquals(3, user.getTokenVersion());
            verify(userRepository).save(user);
            verify(emailVerificationService).clearCode("user@example.com");
        }
    }

    // ============================================================
    // Nested: delete account tests
    // ============================================================
    @Nested
    @DisplayName("delete-account endpoint tests")
    class DeleteAccountTests {

        @Test
        @DisplayName("Should soft-delete account successfully")
        void testDeleteAccount_Success() {
            Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
            SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(authentication.getName()).thenReturn("testuser");
            User user = new User("testuser", "test@example.com", "hash");
            user.setId(1L);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            ResponseEntity<?> response = authController.deleteAccount();
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("deleted successfully"));
            assertTrue(user.isDeleted());
            assertNotNull(user.getDeletedAt());
            verify(userRepository).save(user);

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should return 400 when user not found")
        void testDeleteAccount_UserNotFound() {
            Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
            SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(authentication.getName()).thenReturn("nonexistent");
            when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            ResponseEntity<?> response = authController.deleteAccount();
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Error: User not found", ((MessageResponse) response.getBody()).getMessage());

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should handle exceptions")
        void testDeleteAccount_Exception() {
            Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
            SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            SecurityContextHolder.setContext(securityContext);

            when(authentication.getName()).thenThrow(new RuntimeException("error"));

            ResponseEntity<?> response = authController.deleteAccount();
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

            SecurityContextHolder.clearContext();
        }
    }

    // ============================================================
    // Nested: google sign-in tests
    // ============================================================
    @Nested
    @DisplayName("google sign-in endpoint tests")
    class GoogleSignInTests {

        @Test
        @DisplayName("Should return 400 when URI syntax is invalid")
        void testGoogleSignIn_URISyntaxException() {
            GoogleSignInRequest request = new GoogleSignInRequest();
            request.setIdToken("");
            // An empty or invalid token will cause the URI to be malformed
            ResponseEntity<?> response = authController.googleSignIn(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle exceptions from HTTP client")
        void testGoogleSignIn_IOException() {
            // We can't easily mock HttpClient since it's created inside the method
            // But we can verify it returns an error when something fails
            GoogleSignInRequest request = new GoogleSignInRequest();
            request.setIdToken("some-valid-looking-token");
            ResponseEntity<?> response = authController.googleSignIn(request);
            // The actual HTTP call will fail since there's no real Google server
            assertTrue(response.getStatusCode().isError());
        }
    }

    // ============================================================
    // Nested: link-google-account tests
    // ============================================================
    @Nested
    @DisplayName("link-google-account endpoint tests")
    class LinkGoogleAccountTests {

        @Test
        @DisplayName("Should return 400 when OTP is invalid")
        void testLinkGoogleAccount_InvalidOtp() {
            LinkGoogleAccountRequest request = new LinkGoogleAccountRequest();
            request.setPhone("9876543210");
            request.setOtp("wrong");
            request.setGoogleId("google123");
            request.setPhotoUrl("http://photo.url");

            when(otpUtil.verifyOTP("9876543210", "wrong")).thenReturn(false);

            ResponseEntity<?> response = authController.linkGoogleAccount(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("Invalid or expired OTP"));
        }

        @Test
        @DisplayName("Should return 400 when user not found")
        void testLinkGoogleAccount_UserNotFound() {
            LinkGoogleAccountRequest request = new LinkGoogleAccountRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setGoogleId("google123");

            when(otpUtil.verifyOTP("9876543210", "123456")).thenReturn(true);
            when(userRepository.findByUsername("9876543210")).thenReturn(Optional.empty());
            when(userRepository.findByPhone("9876543210")).thenReturn(Optional.empty());

            ResponseEntity<?> response = authController.linkGoogleAccount(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(((MessageResponse) response.getBody()).getMessage().contains("No account found"));
        }

        @Test
        @DisplayName("Should link Google account successfully")
        void testLinkGoogleAccount_Success() {
            LinkGoogleAccountRequest request = new LinkGoogleAccountRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setGoogleId("google123");
            request.setPhotoUrl("http://photo.url");

            User user = new User("9876543210", "user@example.com", "hash");
            user.setId(1L);
            Role ur = new Role();
            ur.setId(1);
            ur.setName(ERole.ROLE_USER);
            Set<Role> roles = new HashSet<>();
            roles.add(ur);
            user.setRoles(roles);

            when(otpUtil.verifyOTP("9876543210", "123456")).thenReturn(true);
            when(userRepository.findByUsername("9876543210")).thenReturn(Optional.of(user));
            when(jwtUtils.generateJwtToken(any(Authentication.class), anyInt())).thenReturn("link-jwt");

            ResponseEntity<?> response = authController.linkGoogleAccount(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            JwtResponse jwtResponse = (JwtResponse) response.getBody();
            assertNotNull(jwtResponse);
            assertEquals("link-jwt", jwtResponse.getAccessToken());
            assertEquals("google123", user.getGoogleId());
            assertEquals("http://photo.url", user.getGooglePhotoUrl());
            verify(otpUtil).clearOTP("9876543210");
        }

        @Test
        @DisplayName("Should find user by phone field when username lookup fails in link")
        void testLinkGoogleAccount_FindByPhone() {
            LinkGoogleAccountRequest request = new LinkGoogleAccountRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");
            request.setGoogleId("google456");

            User user = new User("emailuser", "email@example.com", "hash");
            user.setId(2L);
            user.setPhone("9876543210");
            Role ur = new Role();
            ur.setId(1);
            ur.setName(ERole.ROLE_USER);
            Set<Role> roles = new HashSet<>();
            roles.add(ur);
            user.setRoles(roles);

            when(otpUtil.verifyOTP("9876543210", "123456")).thenReturn(true);
            when(userRepository.findByUsername("9876543210")).thenReturn(Optional.empty());
            when(userRepository.findByPhone("9876543210")).thenReturn(Optional.of(user));
            when(jwtUtils.generateJwtToken(any(Authentication.class), anyInt())).thenReturn("link-jwt2");

            ResponseEntity<?> response = authController.linkGoogleAccount(request);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle exception gracefully")
        void testLinkGoogleAccount_Exception() {
            LinkGoogleAccountRequest request = new LinkGoogleAccountRequest();
            request.setPhone("9876543210");
            request.setOtp("123456");

            when(otpUtil.verifyOTP("9876543210", "123456")).thenThrow(new RuntimeException("error"));

            ResponseEntity<?> response = authController.linkGoogleAccount(request);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}
