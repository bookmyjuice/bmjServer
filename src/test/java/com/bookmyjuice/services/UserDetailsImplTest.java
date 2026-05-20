package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.bookmyjuice.models.ERole;
import com.bookmyjuice.models.Role;
import com.bookmyjuice.models.User;
import java.util.HashSet;
import java.util.Set;

class UserDetailsImplTest {

    @Nested
    @DisplayName("build() factory method tests")
    class BuildTests {

        @Test
        @DisplayName("Should build UserDetailsImpl from User entity")
        void testBuild_Success() {
            User user = new User("testuser", "test@example.com", "password");
            user.setId(1L);
            Role role = new Role();
            role.setId(1);
            role.setName(ERole.ROLE_USER);
            Set<Role> roles = new HashSet<>();
            roles.add(role);
            user.setRoles(roles);

            UserDetailsImpl userDetails = UserDetailsImpl.build(user);

            assertNotNull(userDetails);
            assertEquals(1L, userDetails.getId());
            assertEquals("testuser", userDetails.getUsername());
            assertEquals("test@example.com", userDetails.getEmail());
            assertEquals("password", userDetails.getPassword());
            assertEquals(1, userDetails.getAuthorities().size());
            assertEquals("ROLE_USER", userDetails.getAuthorities().iterator().next().getAuthority());
        }

        @Test
        @DisplayName("Should build with multiple roles")
        void testBuild_MultipleRoles() {
            User user = new User("admin", "admin@example.com", "password");
            user.setId(2L);
            Role userRole = new Role();
            userRole.setId(1);
            userRole.setName(ERole.ROLE_USER);
            Role adminRole = new Role();
            adminRole.setId(2);
            adminRole.setName(ERole.ROLE_ADMIN);
            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            roles.add(adminRole);
            user.setRoles(roles);

            UserDetailsImpl userDetails = UserDetailsImpl.build(user);
            assertEquals(2, userDetails.getAuthorities().size());
        }
    }

    @Nested
    @DisplayName("Account status methods tests")
    class AccountStatusTests {

        @Test
        @DisplayName("All account status methods should return true")
        void testAccountStatus() {
            UserDetailsImpl userDetails = new UserDetailsImpl(
                    1L, "testuser", "test@example.com", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            assertTrue(userDetails.isAccountNonExpired());
            assertTrue(userDetails.isAccountNonLocked());
            assertTrue(userDetails.isCredentialsNonExpired());
            assertTrue(userDetails.isEnabled());
        }
    }

    @Nested
    @DisplayName("equals() and hashCode() tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Same id should be equal")
        void testEquals_SameId() {
            UserDetailsImpl u1 = new UserDetailsImpl(
                    1L, "user1", "email1@test.com", "pass1",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            UserDetailsImpl u2 = new UserDetailsImpl(
                    1L, "user2", "email2@test.com", "pass2",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            assertEquals(u1, u2);
            assertEquals(u1.hashCode(), u2.hashCode());
        }

        @Test
        @DisplayName("Different ids should not be equal")
        void testEquals_DifferentId() {
            UserDetailsImpl u1 = new UserDetailsImpl(
                    1L, "user1", "email1@test.com", "pass1",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            UserDetailsImpl u2 = new UserDetailsImpl(
                    2L, "user2", "email2@test.com", "pass2",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            assertNotEquals(u1, u2);
        }

        @Test
        @DisplayName("Same object should be equal to itself")
        void testEquals_SameObject() {
            UserDetailsImpl u1 = new UserDetailsImpl(
                    1L, "user1", "email1@test.com", "pass1",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            assertEquals(u1, u1);
        }

        @Test
        @DisplayName("Should not equal null")
        void testEquals_Null() {
            UserDetailsImpl u1 = new UserDetailsImpl(
                    1L, "user1", "email1@test.com", "pass1",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            assertFalse(u1.equals(null));
        }

        @Test
        @DisplayName("Should not equal different class")
        void testEquals_DifferentClass() {
            UserDetailsImpl u1 = new UserDetailsImpl(
                    1L, "user1", "email1@test.com", "pass1",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));

            assertFalse(u1.equals("string"));
        }
    }
}
