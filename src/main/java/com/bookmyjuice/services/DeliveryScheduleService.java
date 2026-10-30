package com.bookmyjuice.services;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bookmyjuice.models.entities.DeliveryScheduleEntity;
import com.bookmyjuice.repository.DeliveryScheduleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Persists the day-wise delivery schedule chosen at subscription checkout and
 * derives {@code nextDeliveryAt} from it.
 *
 * <p>
 * Truthfulness rules this class enforces:
 * </p>
 * <ul>
 * <li>A date is only ever produced from a schedule the customer actually
 * submitted. Nothing is inferred from {@code nextBillingAt}: a weekly billing
 * cycle does not prove the delivery cadence.</li>
 * <li>When no schedule is stored for the subscription, {@code null} is
 * returned so the client renders its em dash instead of a promise.</li>
 * <li>The schedule is written before the Chargebee hosted page is created, so
 * it cannot be lost between checkout and the webhook.</li>
 * </ul>
 */
@Service
public class DeliveryScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryScheduleService.class);

    /** Deliveries are scheduled on the storefront's operating calendar. */
    public static final ZoneId DELIVERY_ZONE = ZoneId.of("Asia/Kolkata");

    /** Subscription id prefix for ids reserved by this backend. */
    private static final String ID_PREFIX = "bmj";

    /**
     * Chargebee statuses that can still deliver. {@code paused} / {@code
     * cancelled} must not publish a date.
     */
    private static final Set<String> DELIVERABLE_STATUSES = Set.of(
            "active", "non_renewing", "future", "in_trial");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, DayOfWeek> DAY_NAMES;

    static {
        Map<String, DayOfWeek> names = new LinkedHashMap<>();
        names.put("monday", DayOfWeek.MONDAY);
        names.put("tuesday", DayOfWeek.TUESDAY);
        names.put("wednesday", DayOfWeek.WEDNESDAY);
        names.put("thursday", DayOfWeek.THURSDAY);
        names.put("friday", DayOfWeek.FRIDAY);
        names.put("saturday", DayOfWeek.SATURDAY);
        names.put("sunday", DayOfWeek.SUNDAY);
        DAY_NAMES = Collections.unmodifiableMap(names);
    }

    @Autowired
    private DeliveryScheduleRepository deliveryScheduleRepository;

    /**
     * Stores the schedule and returns a subscription id to hand to Chargebee
     * ({@code subscription[id]} on the hosted page).
     *
     * @param customerId Chargebee customer id the checkout belongs to
     * @param schedule   day-wise schedule; either {@code {"monday": true}} or
     *                   {@code {"monday": "<juice id>"}}
     * @return the reserved subscription id, or {@code null} when the schedule
     *         carries no delivery day (nothing useful to persist)
     */
    @Transactional
    public String reserve(String customerId, Map<String, ?> schedule) {
        Map<String, String> normalised = normalise(schedule);
        if (normalised.isEmpty()) {
            logger.debug("No delivery days in schedule for customer {} - nothing to persist", customerId);
            return null;
        }

        String subscriptionId = ID_PREFIX + UUID.randomUUID().toString().replace("-", "");

        DeliveryScheduleEntity entity = new DeliveryScheduleEntity();
        entity.setSubscriptionId(subscriptionId);
        entity.setCustomerId(customerId);
        entity.setDaySchedule(toJson(normalised));
        entity.setCreatedAt((int) (System.currentTimeMillis() / 1000L));
        deliveryScheduleRepository.save(entity);

        logger.info("Persisted delivery schedule for reserved subscription {} (customer {}): {}",
                subscriptionId, customerId, entity.getDaySchedule());
        return subscriptionId;
    }

    /** Stored schedule for a subscription; empty when none was captured. */
    @Transactional(readOnly = true)
    public Map<String, String> scheduleFor(String subscriptionId) {
        return find(subscriptionId).map(this::parseSchedule).orElseGet(Collections::emptyMap);
    }

    /**
     * Next delivery date (epoch seconds, IST midnight) for a subscription, or
     * {@code null} when no schedule is stored.
     *
     * @param subscriptionId Chargebee subscription id
     * @param startedAt      subscription start (epoch seconds); may be 0/absent
     */
    @Transactional(readOnly = true)
    public Integer nextDeliveryAt(String subscriptionId, Integer startedAt) {
        Optional<DeliveryScheduleEntity> stored = find(subscriptionId);
        if (stored.isEmpty()) {
            return null;
        }
        return nextDeliveryAt(parseSchedule(stored.get()), startedAt, Instant.now());
    }

    private Optional<DeliveryScheduleEntity> find(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isEmpty()) {
            return Optional.empty();
        }
        return deliveryScheduleRepository.findById(subscriptionId);
    }

    private Map<String, String> parseSchedule(DeliveryScheduleEntity entity) {
        try {
            return parse(entity.getDaySchedule());
        } catch (Exception e) {
            logger.warn("Stored delivery schedule for {} is not readable JSON: {}",
                    entity.getSubscriptionId(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Next delivery date, but only while the subscription is in a status that
     * can still deliver. A paused or cancelled subscription promises nothing.
     *
     * @param subscriptionId Chargebee subscription id
     * @param startedAt      subscription start (epoch seconds)
     * @param status         Chargebee subscription status (any case)
     */
    @Transactional(readOnly = true)
    public Integer nextDeliveryAt(String subscriptionId, Integer startedAt, String status) {
        if (!canPromiseDelivery(status)) {
            return null;
        }
        return nextDeliveryAt(subscriptionId, startedAt);
    }

    /** Whether a delivery date may be published for this subscription status. */
    public static boolean canPromiseDelivery(String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        return DELIVERABLE_STATUSES.contains(status.trim().toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------
    // Pure helpers (unit-tested without Spring)
    // ------------------------------------------------------------------

    /**
     * Keeps only real delivery days and lower-cases the names.
     *
     * <p>
     * Accepts the two shapes the clients send: booleans
     * ({@code {"Monday": true}}) and juice ids
     * ({@code {"monday": "mix-punch"}}). A day is a delivery day when the value
     * is truthy and not blank, so an empty string means "no delivery".
     * </p>
     */
    public static Map<String, String> normalise(Map<String, ?> schedule) {
        Map<String, String> normalised = new LinkedHashMap<>();
        if (schedule == null) {
            return normalised;
        }
        for (Map.Entry<String, ?> entry : schedule.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String day = entry.getKey().trim().toLowerCase(Locale.ROOT);
            if (!DAY_NAMES.containsKey(day)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Boolean selected) {
                if (selected) {
                    normalised.put(day, "true");
                }
                continue;
            }
            if (value == null) {
                continue;
            }
            String text = value.toString().trim();
            if (text.isEmpty() || "false".equalsIgnoreCase(text)) {
                continue;
            }
            normalised.put(day, text);
        }
        return normalised;
    }

    /** Serialises an already-normalised schedule; never throws. */
    public static String toJson(Map<String, String> schedule) {
        try {
            return MAPPER.writeValueAsString(schedule);
        } catch (Exception e) {
            logger.warn("Failed to serialise delivery schedule: {}", e.getMessage());
            return "{}";
        }
    }

    /** Parses a stored schedule; returns an empty map for blank/unreadable JSON. */
    public static Map<String, String> parse(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> raw = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            if (raw == null) {
                return Collections.emptyMap();
            }
            return normalise(raw);
        } catch (Exception e) {
            logger.warn("Failed to parse delivery schedule JSON: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** Weekdays that carry a delivery, in ISO order. */
    public static Set<DayOfWeek> deliveryDays(Map<String, String> schedule) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (schedule == null) {
            return days;
        }
        for (String day : schedule.keySet()) {
            DayOfWeek dayOfWeek = DAY_NAMES.get(day.trim().toLowerCase(Locale.ROOT));
            if (dayOfWeek != null) {
                days.add(dayOfWeek);
            }
        }
        return days;
    }

    /**
     * First scheduled delivery day on or after {@code max(now, start)}.
     *
     * @param schedule  normalised day map
     * @param startedAt subscription start in epoch seconds (0/null = today)
     * @param now       reference instant (injected so the rule is testable)
     * @return epoch seconds at midnight IST of the next delivery day, or
     *         {@code null} when the schedule has no delivery day
     */
    public static Integer nextDeliveryAt(Map<String, String> schedule, Integer startedAt, Instant now) {
        Set<DayOfWeek> days = deliveryDays(schedule);
        if (days.isEmpty()) {
            return null;
        }

        LocalDate today = LocalDate.ofInstant(now, DELIVERY_ZONE);
        LocalDate from = today;
        if (startedAt != null && startedAt > 0) {
            LocalDate start = Instant.ofEpochSecond(startedAt.longValue()).atZone(DELIVERY_ZONE).toLocalDate();
            if (start.isAfter(from)) {
                from = start;
            }
        }

        for (int offset = 0; offset < 8; offset++) {
            LocalDate candidate = from.plusDays(offset);
            if (days.contains(candidate.getDayOfWeek())) {
                return Math.toIntExact(candidate.atStartOfDay(DELIVERY_ZONE).toEpochSecond());
            }
        }
        return null;
    }
}
