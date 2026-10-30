package com.bookmyjuice.controllers;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bookmyjuice.models.User;
import com.bookmyjuice.repository.UserRepository;
import com.bookmyjuice.services.UserDetailsImpl;

/**
 * Production profile endpoint (V1 launch freeze).
 *
 * This is the production replacement for the non-production
 * {@code GET /api/test/user} route (see {@link TestController}).
 *
 * It is pure plumbing: it resolves the authenticated local user and returns the
 * same {@link User} payload that {@code /api/test/user} returns, so the Flutter
 * client contract is unchanged. No advanced profile editing features are added.
 *
 * Mapped under both {@code /api/user} and {@code /api/v1/user} so that
 * versioned and unversioned clients resolve the same handler.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping({ "/api/user", "/api/v1/user" })
public class UserProfileController {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /api/user/profile (alias: /api/v1/user/profile)
     *
     * Returns the authenticated customer profile needed by V1.
     * Requires a bearer token; the user is resolved from the security context.
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getProfile() {
        Long userId = getUserIdFromSecurityContext();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> {
                    logger.warn("Profile requested for unknown user id: {}", userId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "User Not Found with id: " + userId));
                });
    }

    /**
     * Resolve the local DB user id from the security context.
     * Mirrors the helper used by {@link TestController}.
     */
    private Long getUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        return null;
    }
}
