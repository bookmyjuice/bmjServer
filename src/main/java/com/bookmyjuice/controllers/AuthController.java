package com.bookmyjuice.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.bookmyjuice.payload.request.LoginRequest;
import com.bookmyjuice.payload.request.PasswordResetRequest;
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
     * Authenticates with BCrypt password validation and returns 15-minute JWT
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@Valid @RequestBody LoginRequest loginRequest) {
        // Try to authenticate user (works with both phone and email as username)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

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

    @PostMapping("/resetpassword")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest passwordResetRequest) {
        if (passwordResetRequest.getPassword() == null || passwordResetRequest.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: password is empty!"));
        } else if (passwordResetRequest.getPassword().length() < 8) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: password is too short!"));
        } else if (passwordResetRequest.getPassword().length() > 20) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: password is too long!"));
        } else if (!passwordResetRequest.getPassword().matches(".*[a-z].*")) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: password must contain at least one lowercase letter!"));
        } else if (!passwordResetRequest.getPassword().matches(".*[A-Z].*")) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: password must contain at least one uppercase letter!"));
        } else if (!passwordResetRequest.getPassword().matches(".*[0-9].*")) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: password must contain at least one number!"));
        } else if (!passwordResetRequest.getPassword().matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: password must contain at least one special character!"));
        } else if (passwordResetRequest.getPassword().matches(".*\\s.*")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: password must not contain spaces!"));
        } else if (passwordResetRequest.getPassword().matches(".*[\\u0000-\\u001F\\u007F].*")) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: password must not contain control characters!"));
        } else {
            User user = userRepository.findByUsername(passwordResetRequest.getUsername())
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: User is not found."));
            }
            user.setPassword(encoder.encode(passwordResetRequest.getPassword()));
            userRepository.save(user);
            return ResponseEntity.ok(new MessageResponse("ok"));
        }
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
