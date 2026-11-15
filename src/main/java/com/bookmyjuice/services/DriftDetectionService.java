package com.bookmyjuice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Read-only drift detection between the BMJ projection and the identity model.
 *
 * <p><b>Why read-only:</b> production runs {@code spring.jpa.hibernate.ddl-auto=validate},
 * so introducing the {@code sync_findings} / {@code sync_cursor} tables requires a
 * migration and cannot be done by adding entities. Detection therefore uses plain SQL and
 * reports through the log, which is safe to deploy everywhere. Persisting findings and
 * repairing them is the next step, gated on that migration.
 *
 * <p>The detectors correspond to D1, D3, D4 and the dangling form of D2 in
 * {@code CHARGEBEE_RECONCILIATION_PLAN.md}. Every count that should be zero is logged at
 * ERROR, so the condition is visible in production logs today.
 *
 * <p>Disabled by default ({@code sync.detection.enabled=false}); enable per environment.
 *
 * <p>References: CHARGEBEE_SYNC_ROADMAP.md Phase 4.
 */
@Service
@ConditionalOnProperty(name = "sync.detection.enabled", havingValue = "true")
public class DriftDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(DriftDetectionService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** D1 — users that own subscriptions but carry no Chargebee customer id. */
    static final String SQL_NULL_LINK_USERS =
            "SELECT COUNT(*) FROM users u "
                    + "WHERE (u.chargebee_customer_id IS NULL OR u.chargebee_customer_id = '') "
                    + "AND EXISTS (SELECT 1 FROM subscription_entity s "
                    + "            WHERE s.customer_id = CAST(u.id AS CHAR))";

    /** D4 — rows keyed on a local user id instead of the Chargebee customer id. */
    static final String SQL_TWIN_KEY_ROWS =
            "SELECT COUNT(*) FROM subscription_entity s "
                    + "JOIN users u ON s.customer_id = CAST(u.id AS CHAR) "
                    + "WHERE u.chargebee_customer_id IS NOT NULL AND u.chargebee_customer_id <> '' "
                    + "AND u.chargebee_customer_id <> s.customer_id";

    /** D3 — subscription rows that no user can reach. */
    static final String SQL_ORPHAN_SUBSCRIPTIONS =
            "SELECT COUNT(*) FROM subscription_entity s "
                    + "LEFT JOIN users u ON u.chargebee_customer_id = s.customer_id "
                    + "WHERE u.id IS NULL";

    /** D2 (dangling) — a link pointing at a customer row that does not exist. */
    static final String SQL_DANGLING_LINKS =
            "SELECT COUNT(*) FROM users u "
                    + "LEFT JOIN customer_entity c ON c.customer_id = u.chargebee_customer_id "
                    + "WHERE u.chargebee_customer_id IS NOT NULL AND u.chargebee_customer_id <> '' "
                    + "AND c.customer_id IS NULL";

    /** Nightly at 02:00 by default; cron is configurable. */
    @Scheduled(cron = "${sync.detection.cron:0 0 2 * * *}")
    public void nightlyDetection() {
        detect().log();
    }

    /** Run every detector once. Never throws: a detector failure is reported as -1. */
    public DriftReport detect() {
        DriftReport report = new DriftReport();
        report.nullLinkUsers = safeCount(SQL_NULL_LINK_USERS, "D1 null-link users");
        report.twinKeyRows = safeCount(SQL_TWIN_KEY_ROWS, "D4 twin-key rows");
        report.orphanSubscriptions = safeCount(SQL_ORPHAN_SUBSCRIPTIONS, "D3 orphan subscriptions");
        report.danglingLinks = safeCount(SQL_DANGLING_LINKS, "D2 dangling links");
        return report;
    }

    private long safeCount(String sql, String label) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class);
            if (value != null && value > 0) {
                logger.error("Chargebee drift detected — {}: {}", label, value);
            } else {
                logger.info("Chargebee drift check — {}: {}", label, value);
            }
            return value == null ? 0 : value;
        } catch (RuntimeException e) {
            logger.error("Chargebee drift check '{}' could not run: {}", label, e.getMessage());
            return -1;
        }
    }

    /** Snapshot of one detection pass. */
    public static final class DriftReport {
        private long nullLinkUsers;
        private long twinKeyRows;
        private long orphanSubscriptions;
        private long danglingLinks;

        public long getNullLinkUsers() {
            return nullLinkUsers;
        }

        public long getTwinKeyRows() {
            return twinKeyRows;
        }

        public long getOrphanSubscriptions() {
            return orphanSubscriptions;
        }

        public long getDanglingLinks() {
            return danglingLinks;
        }

        /** True when every drift class is zero (a value of -1 means "could not check"). */
        public boolean isClean() {
            return nullLinkUsers == 0 && twinKeyRows == 0
                    && orphanSubscriptions == 0 && danglingLinks == 0;
        }

        public long getTotal() {
            return Math.max(nullLinkUsers, 0) + Math.max(twinKeyRows, 0)
                    + Math.max(orphanSubscriptions, 0) + Math.max(danglingLinks, 0);
        }

        public void log() {
            logger.info("Chargebee drift report: nullLinkUsers={}, twinKeyRows={}, "
                            + "orphanSubscriptions={}, danglingLinks={}",
                    nullLinkUsers, twinKeyRows, orphanSubscriptions, danglingLinks);
            if (isClean()) {
                logger.info("Chargebee projection is consistent with the identity model");
            } else {
                logger.error("Chargebee projection drift: {} finding(s); see individual detector logs",
                        getTotal());
            }
        }

        @Override
        public String toString() {
            return "DriftReport{nullLinkUsers=" + nullLinkUsers + ", twinKeyRows=" + twinKeyRows
                    + ", orphanSubscriptions=" + orphanSubscriptions
                    + ", danglingLinks=" + danglingLinks + "}";
        }
    }
}
