package com.bookmyjuice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled work for the Chargebee synchronisation layer.
 *
 * <p>Before this existed the codebase declared <b>no</b> {@code @Scheduled} beans at all:
 * the webhook retry query and helper were present but unreachable, and no reconciliation
 * loop could run. Enabling scheduling here is what makes those mechanisms live.
 *
 * <p>Every scheduled job remains individually switchable through its own property, so
 * enabling the scheduler does not by itself change production behaviour.
 *
 * <p>References: CHARGEBEE_SYNC_ROADMAP.md Phase 3 and Phase 4.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
