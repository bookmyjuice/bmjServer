package com.bookmyjuice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test to verify that the 'test' profile is active and the DataSource is configured.
 * This test asserts that application-test.properties is loaded and CI environment variables are picked up.
 *
 * This test requires a running database (provided by the MySQL service in CI).
 */
@SpringBootTest
@ActiveProfiles("test")
class TestProfileIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertNotNull(dataSource, "DataSource bean must be present in test profile");

        // Verify that application-test.properties is being loaded
        String profileMarker = environment.getProperty("bmj.test.profile");
        assertEquals("active", profileMarker, "Test profile marker should be 'active'");
    }
}
