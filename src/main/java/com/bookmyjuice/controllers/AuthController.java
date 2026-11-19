package com.bookmyjuice.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
import com.bookmyjuice.models.entities.UserAddressEntity;
import com.bookmyjuice.payload.request.EmailSignupRequest;
import com.bookmyjuice.payload.request.EmailVerificationRequest;
import com.bookmyjuice.payload.request.GoogleSignInRequest;
import com.bookmyjuice.payload.request.LinkGoogleAccountRequest;
import com.bookmyjuice.payload.request.GoogleSignupRequest;
import com.bookmyjuice.payload.request.LoginRequest;
import com.bookmyjuice.payload.request.PhoneSignupRequest;
import com.bookmyjuice.payload.request.ResetPasswordEmailRequest;
import com.bookmyjuice.payload.request.ResetPasswordMobileRequest;
import com.bookmyjuice.payload.request.UnifiedSignupRequest;
import com.bookmyjuice.payload.request.VerifyEmailCodeRequest;
import com.bookmyjuice.payload.response.JwtResponse;
import com.bookmyjuice.payload.response.MessageResponse;
import com.bookmyjuice.repository.RoleRepository;
import com.bookmyjuice.repository.UserAddressRepository;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.security.jwt.JwtUtils;
import com.bookmyjuice.services.IdentityLinker;
import com.bookmyjuice.services.UserDetailsImpl;
import com.bookmyjuice.util.EmailVerificationService;
import com.bookmyjuice.util.OTPUtil;
import com.chargebee.models.Customer;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping({ "/api/auth", "/api/v1/auth" })
public class AuthController {

    // ─────────────────────────────────────────────────────────
    // Note: Both /api/auth/* and /api/v1/auth/* endpoints exist.
    // /api/v1/auth/login is the canonical login endpoint.
    // /api/auth/signin is kept for backward compatibility.
    // ─────────────────────────────────────────────────────────

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    IdentityLinker identityLinker;

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

    @Autowired
    UserAddressRepository userAddressRepository;

    /** A-3: internal mailbox for phone-only accounts. Never shown, never billed. */
    private static final String PHONE_MAILBOX_DOMAIN = "@phone.bookmyjuice.in";

    /** True when the email is the internal phone-mailbox placeholder (A-3). */
    private boolean isSystemMailbox(String email) {
        return email != null && email.toLowerCase().endsWith(PHONE_MAILBOX_DOMAIN);
    }

    /** Redeem a signup token; null when the token is invalid, expired or wrong-purpose. */
    private String redeemSignupToken(String signupToken) {
        try {
            return jwtUtils.getPhoneFromVerificationToken(signupToken);
        } catch (Exception e) {
            logger.warn("Rejected signup token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Redeem an email-proof token; null when the token is invalid, expired or
     * wrong-purpose. The returned address is lower-cased for comparison.
     */
    private String redeemEmailVerificationToken(String emailVerificationToken) {
        if (emailVerificationToken == null || emailVerificationToken.isBlank()) {
            return null;
        }
        try {
            String email = jwtUtils.getEmailFromVerificationToken(emailVerificationToken.trim());
            return email == null ? null : email.trim().toLowerCase();
        } catch (Exception e) {
            logger.warn("Rejected email verification token: {}", e.getMessage());
            return null;
        }
    }

    /** Trim a nullable string and turn blank into null (optional address fields). */
    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Best-effort Chargebee customer creation. A-3: never for a system mailbox. */
    private void createChargebeeCustomer(User user) {
        if (isSystemMailbox(user.getEmail())) {
            logger.info("Skipping Chargebee customer for {}: system mailbox", user.getUsername());
            return;
        }
        try {
            com.chargebee.Result chargebeeResult = Customer.create()
                    .email(user.getEmail())
                    .firstName(user.getFirstName() != null ? user.getFirstName() : "")
                    .lastName(user.getLastName() != null ? user.getLastName() : "")
                    .phone(user.getPhone() != null ? user.getPhone() : "")
                    .preferredCurrencyCode("INR")
                    .request();
            user.setChargebeeCustomerId(chargebeeResult.customer().id());
            userRepository.save(user);
        } catch (Exception e) {
            logger.warn("Failed to create Chargebee customer for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /** Persist the first delivery address; a failure never fails the signup. */
    private void saveDefaultAddress(User user, String fullName, String phone, String line1,
            String line2, String landmark, String city, String state, String zip) {
        if (line1 == null || line1.isBlank()) {
            // Address is optional at signup; the customer can add it later.
            logger.info("No delivery address supplied for user {} (skipped)", user.getId());
            return;
        }
        try {
            UserAddressEntity address = new UserAddressEntity();
            address.setUserId(user.getId());
            address.setLabel("Home");
            address.setFullName(fullName != null ? fullName : "");
            address.setPhone(phone);
            address.setAddressLine1(line1 != null ? line1 : "");
            address.setAddressLine2(line2 != null ? line2 : "");
            address.setLandmark(landmark != null ? landmark : "");
            address.setCity(city != null ? city : "");
            address.setState(state != null ? state : "");
            address.setPincode(zip != null ? zip : "");
            address.setDefault(true);
            userAddressRepository.save(address);
        } catch (Exception e) {
            logger.warn("Failed to create delivery address for user {}: {}", user.getId(), e.getMessage());
        }
    }

    /** Issue the same JWT shape every other login path returns. */
    private JwtResponse jwtFor(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                role.getName().name()))
                        .collect(Collectors.toList()));
        selfHealIdentityLink(user);
        String jwt = jwtUtils.generateJwtToken(authentication, user.getTokenVersion());
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), roles, true);
    }

    /**
     * Self-heal the Chargebee identity link after a successful authentication.
     *
     * <p>Identity linking used to happen only when a customer was created. A user whose
     * link was missing (or pointing at a dangling customer row) stayed broken forever, and
     * every later read treated that as "no subscription". Running the idempotent
     * {@link IdentityLinker#linkUser(Long)} on login makes the system converge with normal
     * use instead of waiting for a repair job.
     *
     * <p>Deliberately never throws: an identity problem must not lock a user out of their
     * account. Failures are logged and the login proceeds.
     */
    private void selfHealIdentityLink(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        try {
            IdentityLinker.LinkResult result = identityLinker.linkUser(user.getId());
            if (!result.isLinked() && result.getOutcome() != IdentityLinker.Outcome.NO_USER_MATCH) {
                logger.warn("Identity self-heal after login for user {}: {}", user.getId(), result);
            }
        } catch (Exception e) {
            logger.warn("Identity self-heal failed for user {}: {}", user.getId(), e.getMessage());
        }
    }

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
        // BUG FIX: Empty string is not null, check explicitly
        if (jwt == null || jwt.isEmpty() || !jwtUtils.validateJwtToken(jwt)) {
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
        try {
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

            selfHealIdentityLink(user);
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
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Invalid username or password!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Authentication failed - " + e.getMessage()));
        }
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
            boolean userModified = false;
            if (user.getGoogleId() == null || user.getGoogleId().isEmpty()) {
                user.setGoogleId(googleId);
                user.setGooglePhotoUrl(photoUrl);
                userModified = true;
            }

            // Step 7: If user has no Chargebee customer, create one now
            if (!isSystemMailbox(user.getEmail())
                && (user.getChargebeeCustomerId() == null || user.getChargebeeCustomerId().isEmpty())) {
                try {
                    // Try to capture firstName/lastName from Google if user doesn't have them
                    if (user.getFirstName() == null || user.getFirstName().isEmpty()) {
                        user.setFirstName(firstName != null ? firstName : email.split("@")[0]);
                        userModified = true;
                    }
                    if (user.getLastName() == null || user.getLastName().isEmpty()) {
                        user.setLastName(lastName != null ? lastName : "");
                        userModified = true;
                    }

                    com.chargebee.Result chargebeeResult = Customer.create()
                            .email(user.getEmail())
                            .firstName(user.getFirstName() != null ? user.getFirstName() : "")
                            .lastName(user.getLastName() != null ? user.getLastName() : "")
                            .phone(user.getPhone() != null ? user.getPhone() : "")
                            .preferredCurrencyCode("INR")
                            .request();
                    Customer chargebeeCustomer = chargebeeResult.customer();
                    user.setChargebeeCustomerId(chargebeeCustomer.id());
                    userModified = true;
                } catch (Exception e) {
                    logger.warn("Failed to create Chargebee customer for Google user {}: {}", user.getEmail(),
                            e.getMessage());
                }
            }

            if (userModified) {
                userRepository.save(user);
            }

            // Step 8: Generate JWT for our system with token version
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null, user.getRoles().stream()
                            .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    role.getName().name()))
                            .collect(Collectors.toList()));

            // 2. Set it into the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            selfHealIdentityLink(user);
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

            // Step 4: If user has no Chargebee customer, create one now
            if (!isSystemMailbox(user.getEmail())
                && (user.getChargebeeCustomerId() == null || user.getChargebeeCustomerId().isEmpty())) {
                try {
                    com.chargebee.Result chargebeeResult = Customer.create()
                            .email(user.getEmail())
                            .firstName(user.getFirstName() != null ? user.getFirstName() : "")
                            .lastName(user.getLastName() != null ? user.getLastName() : "")
                            .phone(user.getPhone() != null ? user.getPhone() : "")
                            .preferredCurrencyCode("INR")
                            .request();
                    Customer chargebeeCustomer = chargebeeResult.customer();
                    user.setChargebeeCustomerId(chargebeeCustomer.id());
                } catch (Exception e) {
                    logger.warn("Failed to create Chargebee customer for linked Google user {}: {}", user.getEmail(),
                            e.getMessage());
                }
            }

            userRepository.save(user);

            otpUtil.clearOTP(request.getPhone());

            // Step 5: Generate JWT with token version
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null, user.getRoles().stream()
                            .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                    role.getName().name()))
                            .collect(Collectors.toList()));

            selfHealIdentityLink(user);
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
                // Step 3 (D-2): Backfill the Chargebee customer if this account never got one.
                // Mirrors the Google sign-in repair block so an OTP sign-in can no longer
                // dead-end at checkout with "Chargebee customer ID not found"
                // (CheckoutService.java:68, SubscriptionCheckoutService.java:87-89).
                // Best-effort: a Chargebee failure is logged and the sign-in still succeeds.
                if (!isSystemMailbox(user.getEmail())
                && (user.getChargebeeCustomerId() == null || user.getChargebeeCustomerId().isEmpty())) {
                    try {
                        com.chargebee.Result chargebeeResult = Customer.create()
                                .email(user.getEmail())
                                .firstName(user.getFirstName() != null ? user.getFirstName() : "")
                                .lastName(user.getLastName() != null ? user.getLastName() : "")
                                .phone(user.getPhone() != null ? user.getPhone() : request.getPhone())
                                .preferredCurrencyCode("INR")
                                .request();
                        Customer chargebeeCustomer = chargebeeResult.customer();
                        user.setChargebeeCustomerId(chargebeeCustomer.id());
                        userRepository.save(user);
                        logger.info("Backfilled Chargebee customer {} for OTP user {}",
                                chargebeeCustomer.id(), user.getUsername());
                    } catch (Exception e) {
                        logger.warn("Failed to backfill Chargebee customer for OTP user {}: {}",
                                user.getEmail(), e.getMessage());
                    }
                }

                // User exists - generate JWT and login
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user.getUsername(), null, user.getRoles().stream()
                                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        role.getName().name()))
                                .collect(Collectors.toList()));

                selfHealIdentityLink(user);
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
                String signupToken = jwtUtils.generatePhoneVerificationToken(request.getPhone());
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("status", "user_not_found");
                body.put("message", "OTP verified. No account found - please complete signup.");
                body.put("phone", request.getPhone());
                body.put("signupToken", signupToken);
                return ResponseEntity.ok(body);
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
    /**
     * A-1: passwordless phone-first signup.
     *
     * Two proofs are required: the phone OTP (via `signupToken`) and the email
     * OTP (via `emailVerificationToken`). The email is mandatory since the
     * mandatory-email redesign, which also guarantees that a Chargebee customer
     * can be provisioned for every account (DP-3). The address is optional.
     */
    @PostMapping("/phone-signup")
    public ResponseEntity<?> phoneSignup(@Valid @RequestBody PhoneSignupRequest request) {
        String phone = redeemSignupToken(request.getSignupToken());
        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                    "Error: Phone verification expired. Please verify your number again."));
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        String verifiedEmail = redeemEmailVerificationToken(request.getEmailVerificationToken());
        if (verifiedEmail == null || verifiedEmail.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                    "Error: Email verification expired. Please verify your email again."));
        }
        if (!verifiedEmail.equalsIgnoreCase(email)) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                    "Error: The verified email does not match the email on this request. Please verify it again."));
        }

        if (userRepository.existsByUsername(phone) || userRepository.findByPhone(phone).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Phone number is already registered!"));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email is already registered!"));
        }

        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName() == null ? "" : request.getLastName().trim();

        // Random, unrecoverable password: the column is NOT NULL but no UI can
        // ever use it. Accounts are reached by Google or phone OTP only.
        User user = new User(phone, email, encoder.encode(java.util.UUID.randomUUID().toString()));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setAddress(blankToNull(request.getAddress()));
        user.setExtendedAddr(blankToNull(request.getExtendedAddr()));
        user.setExtendedAddr2(blankToNull(request.getExtendedAddr2()));
        user.setCity(blankToNull(request.getCity()));
        user.setState(blankToNull(request.getState()));
        user.setZip(blankToNull(request.getZip()));
        user.setCountry(blankToNull(request.getCountry()));

        Set<com.bookmyjuice.models.Role> roles = new HashSet<>();
        com.bookmyjuice.models.Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: ROLE_USER is not found."));
        roles.add(userRole);
        user.setRoles(roles);
        userRepository.save(user);

        createChargebeeCustomer(user);
        saveDefaultAddress(
                user,
                (firstName + " " + lastName).trim(),
                phone,
                request.getAddress(),
                request.getExtendedAddr(),
                request.getExtendedAddr2(),
                request.getCity(),
                request.getState(),
                request.getZip());

        otpUtil.clearOTP(phone);
        logger.info("Phone-first account created for {} ({})", phone, email);
        return ResponseEntity.ok(jwtFor(user));
    }

    /**
     * A-1: passwordless Google-first signup.
     *
     * The Google identity is verified from the ID token on the server (never
     * trusted from the body); the phone comes from the verified signup token.
     */
    @PostMapping("/google-signup")
    public ResponseEntity<?> googleSignup(@Valid @RequestBody GoogleSignupRequest request) {
        String phone = redeemSignupToken(request.getSignupToken());
        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                    "Error: Phone verification expired. Please verify your number again."));
        }

        String email = null;
        String firstName = null;
        String lastName = null;
        String photoUrl = null;
        String googleId = null;

        try {
            java.net.URI uri = new java.net.URI(
                    String.format("https://oauth2.googleapis.com/tokeninfo?id_token=%s",
                            java.net.URLEncoder.encode(request.getIdToken(),
                                    java.nio.charset.StandardCharsets.UTF_8)));
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

            com.fasterxml.jackson.databind.JsonNode tokenInfo =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
            email = tokenInfo.path("email").asText(null);
            firstName = tokenInfo.path("given_name").asText(null);
            lastName = tokenInfo.path("family_name").asText(null);
            photoUrl = tokenInfo.path("picture").asText(null);
            googleId = tokenInfo.path("sub").asText(null);
            boolean emailVerified = tokenInfo.path("email_verified").asBoolean(false);

            if (!emailVerified) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Email not verified by Google"));
            }
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

        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Email not provided by Google"));
        }
        if (googleId == null || googleId.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Google account id missing"));
        }

if (userRepository.existsByUsername(phone) || userRepository.findByPhone(phone).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Phone number is already registered!"));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(
                            "Error: Email is already registered! Sign in with Google instead."));
        }
        for (User existing : userRepository.findAll()) {
            if (googleId.equals(existing.getGoogleId())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse(
                                "Error: This Google account is already linked to another account."));
            }
        }

        String resolvedFirstName = (firstName == null || firstName.isEmpty())
                ? email.split("@")[0]
                : firstName;
        String resolvedLastName = lastName == null ? "" : lastName;

        // Random, unrecoverable password: the column is NOT NULL but no UI can
        // ever use it. Accounts are reached by Google or phone OTP only.
        User user = new User(phone, email.toLowerCase().trim(),
                encoder.encode(java.util.UUID.randomUUID().toString()));
        user.setFirstName(resolvedFirstName);
        user.setLastName(resolvedLastName);
        user.setPhone(phone);
        user.setGoogleId(googleId);
        user.setGooglePhotoUrl(photoUrl);
        user.setAddress(request.getAddress());
        user.setExtendedAddr(request.getExtendedAddr());
        user.setExtendedAddr2(request.getExtendedAddr2());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setZip(request.getZip());
        user.setCountry(request.getCountry());

        Set<com.bookmyjuice.models.Role> roles = new HashSet<>();
        com.bookmyjuice.models.Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: ROLE_USER is not found."));
        roles.add(userRole);
        user.setRoles(roles);
        userRepository.save(user);

        createChargebeeCustomer(user);
        saveDefaultAddress(
                user,
                (resolvedFirstName + " " + resolvedLastName).trim(),
                phone,
                request.getAddress(),
                request.getExtendedAddr(),
                request.getExtendedAddr2(),
                request.getCity(),
                request.getState(),
                request.getZip());

        otpUtil.clearOTP(phone);
        logger.info("Google-first account created for {} ({})", phone, email);
        return ResponseEntity.ok(jwtFor(user));
    }

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

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@Valid @RequestBody SendOTPRequest request) {
        try {
            // BUG FIX: Check rate limiting before generating OTP
            if (!otpUtil.canSendOTP(request.getPhone())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse(
                                "Error: Too many OTP requests. Please wait before requesting a new OTP."));
            }
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
     * Send an email verification code (signup step: the email is mandatory).
     *
     * Responds 429 when a code was issued for the same address less than 30
     * seconds ago, which keeps the public endpoint from being used to email-bomb.
     */
    @PostMapping("/send-email-verification")
    public ResponseEntity<?> sendEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        try {
            // Check if email is already registered
            if (userRepository.existsByEmail(email)) {
                return ResponseEntity
                        .badRequest()
                        .body(new MessageResponse("Error: Email is already registered!"));
            }

            emailVerificationService.generateVerificationCode(email);
            logger.info("Email verification code sent to {}", email);
            return ResponseEntity.ok(new MessageResponse("Success: Verification code sent to " + email));
        } catch (IllegalStateException cooldown) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MessageResponse("Error: " + cooldown.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to send email verification code to {}: {}", email, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Failed to send verification code - " + e.getMessage()));
        }
    }

    /**
     * Verify an email verification code (signup step).
     *
     * On success the code is consumed and the response carries
     * {@code emailVerificationToken}: the signed, 15-minute proof that
     * `/auth/phone-signup` requires next to the phone `signupToken`.
     */
    @PostMapping("/verify-email-code")
    public ResponseEntity<?> verifyEmailCode(@Valid @RequestBody VerifyEmailCodeRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        try {
            if (emailVerificationService.verifyCode(email, request.getVerificationCode())) {
                emailVerificationService.clearCode(email);
                Map<String, Object> body = new java.util.LinkedHashMap<>();
                body.put("message", "Success: Email verified!");
                body.put("email", email);
                body.put("emailVerificationToken", jwtUtils.generateEmailVerificationToken(email));
                return ResponseEntity.ok(body);
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
        // BUG FIX: Validate password complexity (was missing in unified-signup)
        String pwValidation = validatePassword(request.getPassword());
        if (pwValidation != null) {
            return ResponseEntity.badRequest().body(new MessageResponse(pwValidation));
        }

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

        // Use phone as username (10-digit phone number)
        String username = request.getPhone().trim();

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

        // Store Google-specific fields if provided (for Google signup flow)
        if (request.getGoogleId() != null && !request.getGoogleId().isEmpty()) {
            user.setGoogleId(request.getGoogleId());
        }
        if (request.getPhotoUrl() != null && !request.getPhotoUrl().isEmpty()) {
            user.setGooglePhotoUrl(request.getPhotoUrl());
        }

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
            com.chargebee.Result chargebeeResult = Customer.create()
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
            // Store the Chargebee customer ID back to the user record
            Customer chargebeeCustomer = chargebeeResult.customer();
            user.setChargebeeCustomerId(chargebeeCustomer.id());
            userRepository.save(user);
        } catch (Exception e) {
            // Rollback: delete user if Chargebee creation fails
            userRepository.delete(user);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Error: Failed to create Chargebee customer - " + e.getMessage()));
        }

        // BUG-006 FIX: Save delivery address to user_addresses table
        // so checkout flow can find it (was missing: signup stored address only in
        // users table)
        try {
            UserAddressEntity deliveryAddress = new UserAddressEntity();
            deliveryAddress.setUserId(user.getId());
            deliveryAddress.setLabel("Home");
            deliveryAddress.setFullName(
                    (request.getFirstName() != null ? request.getFirstName() : "") + " " +
                            (request.getLastName() != null ? request.getLastName() : ""));
            deliveryAddress.setPhone(request.getPhone());
            deliveryAddress.setAddressLine1(request.getAddress() != null ? request.getAddress() : "");
            deliveryAddress.setAddressLine2(request.getExtendedAddr() != null ? request.getExtendedAddr() : "");
            deliveryAddress.setLandmark(request.getExtendedAddr2() != null ? request.getExtendedAddr2() : "");
            deliveryAddress.setCity(request.getCity() != null ? request.getCity() : "");
            deliveryAddress.setState(request.getState() != null ? request.getState() : "");
            deliveryAddress.setPincode(request.getZip() != null ? request.getZip() : "");
            deliveryAddress.setDefault(true);
            userAddressRepository.save(deliveryAddress);
            logger.info("BUG-006 FIX: Created delivery address for user {} ({})", user.getId(), request.getPhone());
        } catch (Exception e) {
            // Log but don't fail signup - address can be added later
            logger.warn("BUG-006 FIX: Failed to create delivery address for user {}: {}", user.getId(), e.getMessage());
        }

        // Clear OTP if exists
        otpUtil.clearOTP(request.getPhone());

        selfHealIdentityLink(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    /**
     * FR-AUTH-001: Email-based user signup (Legacy - keep for backward
     * compatibility)
     * Creates a local user account with BCrypt hashed password and syncs to
     * Chargebee.
     * Username is set to the user's 10-digit phone number.
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody EmailSignupRequest request) {
        // Validate email not already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already registered!"));
        }

        // Use phone as username (10-digit phone number)
        String username = request.getPhone().trim();

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
            com.chargebee.Result chargebeeResult = Customer.create()
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phone(user.getPhone())
                    .preferredCurrencyCode("INR")
                    .request();
            // Store the Chargebee customer ID back to the user record
            Customer chargebeeCustomer = chargebeeResult.customer();
            user.setChargebeeCustomerId(chargebeeCustomer.id());
            userRepository.save(user);
        } catch (Exception e) {
            // Rollback: delete user if Chargebee creation fails
            userRepository.delete(user);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Error: Failed to create Chargebee customer - " + e.getMessage()));
        }

        selfHealIdentityLink(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }
}
