package com.bookmyjuice.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.dto.request.SendOTPRequest;
import com.bookmyjuice.dto.request.VerifyOTPRequest;
import com.bookmyjuice.models.ERole;
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
import com.chargebee.models.Customer;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    OTPUtil otpUtil;

    @Autowired
    EmailVerificationService emailVerificationService;

    @Autowired
    JwtUtils jwtUtils;

    @Value("${google.client.id}")

    @GetMapping("/autologin")
    public ResponseEntity<?> autoLogin(@RequestHeader(value = "Authorization", required = false) String authorization) {
        // headers.get("Authorization");`````````````
        if (authorization == null || authorization.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Authorization header is missing!"));
        }
        if (authorization.length() < 8) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Authorization header is invalid!"));
        }
        String jwt = authorization.substring(7);
        if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid JWT token!"));
        } else if (jwtUtils.getUserNameFromJwtToken(jwt) == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: JWT token is expired!"));
        } else if (jwtUtils.getUserNameFromJwtToken(jwt).isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: JWT token is unsupported!"));
        } else if (jwtUtils.getUserNameFromJwtToken(jwt).isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: JWT claims string is empty!"));
        } else {
            return ResponseEntity.ok(new MessageResponse("ok"));
        }
    }

    /**
     * FR-AUTH-002: User login (supports both phone and email)
     * Authenticates with BCrypt password validation and returns JWT with token
     * version.
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@Valid @RequestBody LoginRequest loginRequest) {
        // Try to authenticate user (works with both phone and email as username)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get user's token version for JWT invalidation support
        User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
        if (user == null) {
            user = userRepository.findByEmail(loginRequest.getUsername()).orElse(null);
        }
        int tokenVersion = (user != null) ? user.getTokenVersion() : 1;

        String jwt = jwtUtils.generateJwtToken(authentication, tokenVersion);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    /**
     * FR-AUTH-003: Google Sign-In
     * Verifies Google ID token via Google's tokeninfo endpoint,
     * creates/finds user, and returns JWT for our system.
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleSignIn(@Valid @RequestBody GoogleSignInRequest request) {
        try {
            // Step 1: Verify Google ID token by calling Google's tokeninfo endpoint
            java.net.URI uri = new java.net.URI(
                    String.format("https://oauth2.googleapis.com/tokeninfo?id_token=%s",
                            java.net.URLEncoder.encode(request.getIdToken(), java.nio.charset.StandardCharsets.UTF_8)));
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(httpRequest,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Invalid Google ID token"));
            }

            // Step 2: Parse token info from Google response
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode tokenInfo = mapper.readTree(response.body());
            String email = tokenInfo.path("email").asText(null);
            String firstName = tokenInfo.path("given_name").asText(null);
            String lastName = tokenInfo.path("family_name").asText(null);
            String photoUrl = tokenInfo.path("picture").asText(null);
            String googleId = tokenInfo.path("sub").asText(null);
            boolean emailVerified = tokenInfo.path("email_verified").asBoolean(false);

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Email not provided by Google"));
            }
            if (!emailVerified) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Email not verified by Google"));
            }

            // Step 3: Find user by Google ID first
            User user = null;
            for (User u : userRepository.findAll()) {
                if (googleId.equals(u.getGoogleId())) {
                    user = u;
                    break;
                }
            }

            // Step 4: If not found by Google ID, try email
            if (user == null) {
                user = userRepository.findByEmail(email).orElse(null);
            }

            // Step 5: If still not found, return link_required for intermediate screen
            if (user == null) {
                return ResponseEntity.ok(Map.of(
                        "status", "link_required",
                        "message",
                        "No account found with this Google account. Link to existing account or create new one.",
                        "googleEmail", email,
                        "googleFirstName", firstName != null ? firstName : "",
                        "googleLastName", lastName != null ? lastName : "",
                        "googleId", googleId));
            }

            // Step 6: User found - link Google ID if not already linked
            if (user.getGoogleId() == null || user.getGoogleId().isEmpty()) {
                user.setGoogleId(googleId);
                user.setGooglePhotoUrl(photoUrl);
                userRepository.save(user);
            }

            // Step 4: Generate JWT for our system with token version
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null, user.getRoles().stream()
                            .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    role.getName().name()))
                            .collect(Collectors.toList()));

            String jwt = jwtUtils.generateJwtToken(authentication, user.getTokenVersion());
            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            return ResponseEntity
                    .ok(new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), roles, false));

        } catch (java.net.URISyntaxException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Invalid Google token format"));
        } catch (java.io.IOException | InterruptedException e) {
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Error: Google verification service unavailable"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Google authentication failed - " + e.getMessage()));
        }
    }

    /**
     * Link Google account to existing user via phone OTP verification.
     * Prevents duplicate accounts when user signs up with different Google email.
     */
    @PostMapping("/link-google-account")
    public ResponseEntity<?> linkGoogleAccount(@Valid @RequestBody LinkGoogleAccountRequest request) {
        try {
            // Step 1: Verify OTP
            if (!otpUtil.verifyOTP(request.getPhone(), request.getOtp())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Invalid or expired OTP!"));
            }

            // Step 2: Find existing user by phone
            User user = userRepository.findByUsername(request.getPhone()).orElse(null);
            if (user == null) {
                user = userRepository.findByPhone(request.getPhone()).orElse(null);
            }

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: No account found with this phone number."));
            }

            // Step 3: Link Google ID to existing account
            user.setGoogleId(request.getGoogleId());
            user.setGooglePhotoUrl(request.getPhotoUrl());
            userRepository.save(user);

            otpUtil.clearOTP(request.getPhone());

            // Step 4: Generate JWT with token version
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null, user.getRoles().stream()
                            .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    role.getName().name()))
                            .collect(Collectors.toList()));

            String jwt = jwtUtils.generateJwtToken(authentication, user.getTokenVersion());
            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            return ResponseEntity
                    .ok(new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), roles, false));
        } catch (Exception e) {
            logger.error("Error linking Google account: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Failed to link Google account - " + e.getMessage()));
        }
    }

    /**
     * BR-009/#9: Delete user account (soft delete).
     * Marks user as deleted with 30-day grace period.
     * Requires authentication - user must be logged in.
     */
    @DeleteMapping("/account")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteAccount() {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: User not found"));
            }

            // Soft delete
            user.setDeleted(true);
            user.setDeletedAt(java.time.LocalDateTime.now());
            userRepository.save(user);

            logger.info("User {} soft-deleted account", username);

            return ResponseEntity
                    .ok(new MessageResponse("Account deleted successfully. You have 30 days to recover your account."));
        } catch (Exception e) {
            logger.error("Error deleting account: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Error: Failed to delete account"));
        }
    }

    /**
     * BR-011: Phone Sign-In via OTP. Verifies OTP, then logs in existing user or
     * returns "user_not_found".
     * If user exists: returns JWT token
     * If user doesn't exist: returns user_not_found so frontend can start signup
     */
    @PostMapping("/login-otp")
    public ResponseEntity<?> loginViaOtp(@Valid @RequestBody VerifyOTPRequest request) {
        try {
            // Step 1: Verify OTP
            if (!otpUtil.verifyOTP(request.getPhone(), request.getOtp())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Invalid or expired OTP!"));
            }

            // Step 2: Check if user exists with this phone number
            User user = userRepository.findByUsername(request.getPhone()).orElse(null);
            if (user == null) {
                // Also check if phone is stored in phone field for email-first signup users
                user = userRepository.findByPhone(request.getPhone()).orElse(null);
            }

            if (user != null) {
                // User exists - generate JWT and login
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user.getUsername(), null, user.getRoles().stream()
                                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        role.getName().name()))
                                .collect(Collectors.toList()));

                String jwt = jwtUtils.generateJwtToken(authentication, user.getTokenVersion());
                List<String> roles = user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList());

                otpUtil.clearOTP(request.getPhone());

                return ResponseEntity
                        .ok(new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), roles, false));
            } else {
                // User doesn't exist - frontend should start signup flow
                otpUtil.clearOTP(request.getPhone());
                return ResponseEntity.ok(Map.of(
                        "status", "user_not_found",
                        "message", "OTP verified. No account found - please complete signup.",
                        "phone", request.getPhone()));
            }
        } catch (Exception e) {
            logger.error("Error in OTP login: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: OTP login failed - " + e.getMessage()));
        }
    }

    /**
     * BR-009 Method 1: Reset password via mobile OTP.
     * Validates phone number, OTP, then sets new password.
     */
    @PostMapping("/reset-password-mobile")
    public ResponseEntity<?> resetPasswordViaMobile(@Valid @RequestBody ResetPasswordMobileRequest request) {
        String pwValidation = validatePassword(request.getPassword());
        if (pwValidation != null) {
            return ResponseEntity.badRequest().body(new MessageResponse(pwValidation));
        }

        if (!otpUtil.verifyOTP(request.getPhone(), request.getOtp())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Invalid or expired OTP!"));
        }

        User user = userRepository.findByUsername(request.getPhone()).orElse(null);
        if (user == null) {
            user = userRepository.findByEmail(request.getPhone()).orElse(null);
        }
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: No account found with this phone number."));
        }

        user.setPassword(encoder.encode(request.getPassword()));
        // Increment token version to invalidate all existing sessions
        user.invalidateAllTokens();
        userRepository.save(user);
        otpUtil.clearOTP(request.getPhone());

        return ResponseEntity
                .ok(new MessageResponse("Password reset successfully! All existing sessions have been invalidated."));
    }

    /**
     * BR-009 Method 2: Reset password via email verification code.
     * Validates email, verification code, then sets new password.
     */
    @PostMapping("/reset-password-email")
    public ResponseEntity<?> resetPasswordViaEmail(@Valid @RequestBody ResetPasswordEmailRequest request) {
        String pwValidation = validatePassword(request.getPassword());
        if (pwValidation != null) {
            return ResponseEntity.badRequest().body(new MessageResponse(pwValidation));
        }

        if (!emailVerificationService.verifyCode(request.getEmail(), request.getVerificationCode())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Invalid or expired verification code!"));
        }

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: No account found with this email address."));
        }

        user.setPassword(encoder.encode(request.getPassword()));
        // Increment token version to invalidate all existing sessions
        user.invalidateAllTokens();
        userRepository.save(user);
        emailVerificationService.clearCode(request.getEmail());

        return ResponseEntity
                .ok(new MessageResponse("Password reset successfully! All existing sessions have been invalidated."));
    }

    /**
     * Helper: Validate password complexity.
     * Returns null if valid, error message if invalid.
     */
    private String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Error: Password is required.";
        }
        if (password.length() < 8) {
            return "Error: Password must be at least 8 characters.";
        }
        if (password.length() > 40) {
            return "Error: Password must not exceed 40 characters.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Error: Password must contain at least one lowercase letter.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Error: Password must contain at least one uppercase letter.";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Error: Password must contain at least one number.";
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return "Error: Password must contain at least one special character.";
        }
        if (password.matches(".*\\s.*")) {
            return "Error: Password must not contain spaces.";
        }
        return null;
    }

    // private Long getUserIdFromSecurityContext() {
    // Authentication authentication =
    // SecurityContextHolder.getContext().getAuthentication();
    // if (authentication != null && authentication.getPrincipal() instanceof
    // UserDetailsImpl) {
    // UserDetailsImpl userDetails = (UserDetailsImpl)
    // authentication.getPrincipal();
    // return userDetails.getId();
    // }
    // return null; // Or throw an exception
    // }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@Valid @RequestBody SendOTPRequest request) {
        try {
            String otp = otpUtil.generateOTP(request.getPhone());
            return ResponseEntity.ok(new MessageResponse("Success: OTP sent! Check console for OTP (Dev mode)"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Failed to send OTP - " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@Valid @RequestBody VerifyOTPRequest request) {
        try {
            if (otpUtil.verifyOTP(request.getPhone(), request.getOtp())) {
                return ResponseEntity.ok(new MessageResponse("Success: OTP verified!"));
            } else {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Invalid or expired OTP!"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: OTP verification failed - " + e.getMessage()));
        }
    }

    /**
     * Send email verification code (NEW unified signup flow)
     */
    @PostMapping("/send-email-verification")
    public ResponseEntity<?> sendEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
        try {
            // Check if email is already registered
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Email is already registered!"));
            }

            String code = emailVerificationService.generateVerificationCode(request.getEmail());
            return ResponseEntity.ok(new MessageResponse("Success: Verification code sent to " + request.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Failed to send verification code - " + e.getMessage()));
        }
    }

    /**
     * Verify email verification code (NEW unified signup flow)
     */
    @PostMapping("/verify-email-code")
    public ResponseEntity<?> verifyEmailCode(@Valid @RequestBody VerifyEmailCodeRequest request) {
        try {
            if (emailVerificationService.verifyCode(request.getEmail(), request.getVerificationCode())) {
                emailVerificationService.clearCode(request.getEmail());
                return ResponseEntity.ok(new MessageResponse("Success: Email verified!"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Invalid or expired verification code!"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email verification failed - " + e.getMessage()));
        }
    }

    /**
     * Unified signup endpoint (NEW - creates account with email, phone, password,
     * and address)
     */
    @PostMapping("/unified-signup")
    public ResponseEntity<?> unifiedSignup(@Valid @RequestBody UnifiedSignupRequest request) {
        // Validate email not already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already registered!"));
        }

        // Validate phone not already registered
        if (userRepository.existsByUsername(request.getPhone())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Phone number is already registered!"));
        }

        // Use email as username for email-based auth
        String username = request.getEmail().toLowerCase().trim();

        // Create new user with BCrypt hashed password
        User user = new User(
                username,
                request.getEmail().toLowerCase().trim(),
                encoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setExtendedAddr(request.getExtendedAddr());
        user.setExtendedAddr2(request.getExtendedAddr2());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setZip(request.getZip());
        user.setCountry(request.getCountry());

        // Assign default USER role
        Set<com.bookmyjuice.models.Role> roles = new HashSet<>();
        com.bookmyjuice.models.Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: ROLE_USER is not found."));
        roles.add(userRole);
        user.setRoles(roles);

        // Save user to database
        userRepository.save(user);

        // Create Chargebee customer
        try {
            Customer.create()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phone(user.getPhone())
                    .billingAddressFirstName(user.getFirstName())
                    .billingAddressLastName(user.getLastName())
                    .billingAddressLine1(user.getAddress())
                    .billingAddressLine2(user.getExtendedAddr())
                    .billingAddressLine3(user.getExtendedAddr2())
                    .billingAddressCity(user.getCity())
                    .billingAddressState(user.getState())
                    .billingAddressZip(user.getZip())
                    .billingAddressCountry(user.getCountry())
                    .preferredCurrencyCode("INR")
                    .request();
        } catch (Exception e) {
            // Rollback: delete user if Chargebee creation fails
            userRepository.delete(user);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Error: Failed to create Chargebee customer - " + e.getMessage()));
        }

        // Clear OTP if exists
        otpUtil.clearOTP(request.getPhone());

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /**
     * FR-AUTH-001: Email-based user signup (Legacy - keep for backward
     * compatibility)
     * Creates a local user account with BCrypt hashed password and syncs to
     * Chargebee
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody EmailSignupRequest request) {
        // Validate email not already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already registered!"));
        }

        // Use email as username for email-based auth
        String username = request.getEmail().toLowerCase().trim();

        // Create new user with BCrypt hashed password
        User user = new User(
                username,
                request.getEmail().toLowerCase().trim(),
                encoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());

        // Assign default USER role
        Set<com.bookmyjuice.models.Role> roles = new HashSet<>();
        com.bookmyjuice.models.Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: ROLE_USER is not found."));
        roles.add(userRole);
        user.setRoles(roles);

        // Save user to database
        userRepository.save(user);

        // Create Chargebee customer
        try {
            Customer.create()
                    .id(user.getId().toString())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phone(user.getPhone())
                    .preferredCurrencyCode("INR")
                    .request();
        } catch (Exception e) {
            // Rollback: delete user if Chargebee creation fails
            userRepository.delete(user);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Error: Failed to create Chargebee customer - " + e.getMessage()));
        }

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
