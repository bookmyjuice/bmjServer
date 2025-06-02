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

import com.bookmyjuice.models.ERole;
import com.bookmyjuice.models.Role;
import com.bookmyjuice.models.User;
import com.bookmyjuice.payload.request.LoginRequest;
import com.bookmyjuice.payload.request.PasswordResetRequest;
import com.bookmyjuice.payload.request.SignupRequest;
import com.bookmyjuice.payload.response.JwtResponse;
import com.bookmyjuice.payload.response.MessageResponse;
import com.bookmyjuice.repository.RoleRepository;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.security.jwt.JwtUtils;
import com.bookmyjuice.services.UserDetailsImpl;
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

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

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

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {

        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));
        user.setAddress(signUpRequest.getAddress());
        user.setExtendedAddr(signUpRequest.getExtendedAddr());
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setCity(signUpRequest.getCity());
        user.setState(signUpRequest.getState());
        user.setCountry(signUpRequest.getCountry());
        user.setZip(signUpRequest.getZip());
        user.setExtendedAddr2(signUpRequest.getExtendedAddr2());

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin" -> {
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                    }
                    case "mod" -> {
                        Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);
                    }
                    default -> {
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                    }
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        try {
            Customer.create()
                    .id(user.getId().toString())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getUsername())
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
            userRepository.delete(user); // Rollback user creation if Chargebee fails
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Error: Chargebee registration error!"));
        }
        return ResponseEntity.ok(new MessageResponse(user.getId().toString()));
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
            return ResponseEntity.badRequest().body(new MessageResponse("Error: password must contain at least one lowercase letter!"));
        } else if (!passwordResetRequest.getPassword().matches(".*[A-Z].*")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: password must contain at least one uppercase letter!"));
        } else if (!passwordResetRequest.getPassword().matches(".*[0-9].*")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: password must contain at least one number!"));
        } else if (!passwordResetRequest.getPassword().matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: password must contain at least one special character!"));
        } else if (passwordResetRequest.getPassword().matches(".*\\s.*")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: password must not contain spaces!"));
        } else if (passwordResetRequest.getPassword().matches(".*[\\u0000-\\u001F\\u007F].*")) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: password must not contain control characters!"));
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
    //     Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    //     if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
    //         UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    //         return userDetails.getId();
    //     }
    //     return null; // Or throw an exception
    // }
}
