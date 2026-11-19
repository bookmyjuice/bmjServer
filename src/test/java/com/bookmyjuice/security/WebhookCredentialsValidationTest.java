package com.bookmyjuice.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The webhook endpoint must never start unauthenticated.
 *
 * <p>Both credentials were declared as {@code @Value("${WEBHOOK_USERNAME:}")}, i.e. an empty
 * default, so a deployment that forgot them still built an in-memory user with a blank
 * password and {@code /api/webhooks/**} accepted {@code Basic <anything>:}. Startup now fails
 * instead.
 */
class WebhookCredentialsValidationTest {

    private WebSecurityConfig config(String user, String password, boolean optional) {
        WebSecurityConfig cfg = new WebSecurityConfig();
        ReflectionTestUtils.setField(cfg, "webhookUsername", user);
        ReflectionTestUtils.setField(cfg, "webhookPassword", password);
        ReflectionTestUtils.setField(cfg, "webhookAuthOptional", optional);
        return cfg;
    }

    @Test
    @DisplayName("starts normally when both credentials are present")
    void bothPresentIsAccepted() {
        assertDoesNotThrow(() -> config("bmj-webhooks", "s3cret-value", false)
                .validateWebhookCredentials());
    }

    @Test
    @DisplayName("refuses to start when the password is blank")
    void blankPasswordAborts() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> config("bmj-webhooks", "   ", false).validateWebhookCredentials());
        assertTrue(ex.getMessage().contains("WEBHOOK_PASSWORD"));
    }

    @Test
    @DisplayName("refuses to start when the username is blank")
    void blankUsernameAborts() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> config(null, "s3cret-value", false).validateWebhookCredentials());
        assertTrue(ex.getMessage().contains("WEBHOOK_USERNAME"));
    }

    @Test
    @DisplayName("refuses to start when both are missing, naming both")
    void bothMissingAbortsNamingBoth() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> config("", "", false).validateWebhookCredentials());
        assertTrue(ex.getMessage().contains("WEBHOOK_USERNAME"));
        assertTrue(ex.getMessage().contains("WEBHOOK_PASSWORD"));
    }

    @Test
    @DisplayName("the documented local escape hatch still allows a blank configuration")
    void optionalModeAllowsBlank() {
        assertDoesNotThrow(() -> config("", "", true).validateWebhookCredentials());
    }
}
