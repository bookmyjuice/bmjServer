package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.bookmyjuice.models.ERole;
import com.bookmyjuice.models.Role;
import com.bookmyjuice.models.User;
import com.bookmyjuice.repository.UserRepository;

class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("loadUserByUsername tests")
    class LoadUserByUsernameTests {

        private User createTestUser(String username, String email, boolean deleted) {
            User user = new User(username, email, "password");
            user.setId(1L);
            Role userRole = new Role();
            userRole.setId(1);
            userRole.setName(ERole.ROLE_USER);
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            user.setRoles(roles);
            user.setDeleted(deleted);
            return user;
        }

        @Test
        @DisplayName("Should load user by username (phone) successfully")
        void testLoadUserByUsername_Success() {
            User user = createTestUser("9876543210", "user@example.com", false);
            when(userRepository.findByUsername("9876543210")).thenReturn(Optional.of(user));

            UserDetails userDetails = userDetailsService.loadUserByUsername("9876543210");
            assertNotNull(userDetails);
            assertEquals("9876543210", userDetails.getUsername());
            assertEquals("password", userDetails.getPassword());
        }

        @Test
        @DisplayName("Should load user by email when username not found")
        void testLoadUserByEmail_Fallback() {
            User user = createTestUser("testuser", "test@example.com", false);
            when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");
            assertNotNull(userDetails);
            assertEquals("testuser", userDetails.getUsername());
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user not found by username or email")
        void testLoadUserByUsername_NotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> userDetailsService.loadUserByUsername("unknown"));
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user is soft-deleted")
        void testLoadUserByUsername_Deleted() {
            User user = createTestUser("deleteduser", "deleted@example.com", true);
            when(userRepository.findByUsername("deleteduser")).thenReturn(Optional.of(user));

            assertThrows(UsernameNotFoundException.class,
                    () -> userDetailsService.loadUserByUsername("deleteduser"));
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when deleted user found via email")
        void testLoadUserByUsername_DeletedViaEmail() {
            when(userRepository.findByUsername("deleted@example.com")).thenReturn(Optional.empty());
            User user = createTestUser("deleteduser", "deleted@example.com", true);
            when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.of(user));

            assertThrows(UsernameNotFoundException.class,
                    () -> userDetailsService.loadUserByUsername("deleted@example.com"));
        }

        @Test
        @DisplayName("Should build UserDetailsImpl with authorities")
        void testLoadUserByUsername_HasAuthorities() {
            User user = createTestUser("testuser", "test@example.com", false);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");
            assertNotNull(userDetails);
            assertEquals(1, userDetails.getAuthorities().size());
            assertEquals("ROLE_USER", userDetails.getAuthorities().iterator().next().getAuthority());
        }
    }
}
