package com.bookmyjuice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bookmyjuice.models.ERole;
import com.bookmyjuice.models.Role;
import com.bookmyjuice.repository.RoleRepository;

/**
 * Seeds initial roles into the database on application startup.
 * Prevents "ROLE_USER not found" errors during signup.
 */
@Component
public class RoleSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleSeeder.class);
    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        seedRole(ERole.ROLE_USER, "Default user role");
    }

    private void seedRole(ERole roleEnum, String description) {
        String roleName = roleEnum.name();
        if (!roleRepository.existsByName(roleEnum)) {
            Role role = new Role();
            role.setName(roleEnum);
            roleRepository.save(role);
            log.info("Seeded role: {} - {}", roleName, description);
        } else {
            log.debug("Role already exists: {}", roleName);
        }
    }
}
