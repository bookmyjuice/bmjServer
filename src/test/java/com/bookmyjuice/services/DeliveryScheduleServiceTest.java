package com.bookmyjuice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the day-wise delivery schedule rules.
 *
 * <p>
 * These cover the "never invent a date" contract: a date is produced only from
 * a stored schedule, on a scheduled weekday, on or after both today and the
 * subscription start, and never while the subscription is paused/cancelled.
 * </p>
 */
class DeliveryScheduleServiceTest {

    /** 2026-11-02 is a Monday; all expectations below are anchored to it. */
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    private static Instant ist(String isoLocalDateTime) {
        return ZonedDateTime.of(LocalDateTime.parse(isoLocalDateTime),
                DeliveryScheduleService.DELIVERY_ZONE).toInstant();
    }

    private static LocalDate dateOf(Integer epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds.longValue())
                .atZone(DeliveryScheduleService.DELIVERY_ZONE)
                .toLocalDate();
    }

    @Nested
    @DisplayName("normalise")
    class Normalise {

        @Test
        @DisplayName("keeps real delivery days, lower-cases names and drops blank/false values")
        void normalisesMixedShapes() {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("Monday", true);
            raw.put("tuesday", false);
            raw.put("Wednesday", "colon-cleanser");
            raw.put("thursday", "   ");
            raw.put("Friday", "false");
            raw.put("Saturday", null);
            raw.put("Sunday", "mix-punch");
            raw.put("Funday", true);

            Map<String, String> normalised = DeliveryScheduleService.normalise(raw);

            assertEquals(Map.of("monday", "true", "wednesday", "colon-cleanser", "sunday", "mix-punch"),
                    normalised);
        }

        @Test
        @DisplayName("null schedule normalises to an empty map")
        void nullScheduleIsEmpty() {
            assertTrue(DeliveryScheduleService.normalise(null).isEmpty());
        }
    }

    @Nested
    @DisplayName("json round trip")
    class JsonRoundTrip {

        @Test
        @DisplayName("toJson then parse returns the same schedule")
        void roundTrips() {
            Map<String, String> schedule = new LinkedHashMap<>();
            schedule.put("monday", "mix-punch");
            schedule.put("wednesday", "wheatgrass-juice");

            String json = DeliveryScheduleService.toJson(schedule);

            assertEquals("{\"monday\":\"mix-punch\",\"wednesday\":\"wheatgrass-juice\"}", json);
            assertEquals(schedule, DeliveryScheduleService.parse(json));
        }

        @Test
        @DisplayName("blank or unreadable JSON parses to an empty map")
        void unreadableJsonIsEmpty() {
            assertTrue(DeliveryScheduleService.parse(null).isEmpty());
            assertTrue(DeliveryScheduleService.parse("  ").isEmpty());
            assertTrue(DeliveryScheduleService.parse("not-json").isEmpty());
        }
    }

    @Nested
    @DisplayName("nextDeliveryAt")
    class NextDeliveryAt {

        @Test
        @DisplayName("schedule with only Monday from a Wednesday resolves to the following Monday")
        void mondayOnly() {
            Map<String, String> schedule = Map.of("monday", "mix-punch");

            Integer next = DeliveryScheduleService.nextDeliveryAt(schedule, 0,
                    ist("2026-11-04T10:00:00"));

            assertEquals(MONDAY.plusDays(7), dateOf(next));
            assertEquals(0, Instant.ofEpochSecond(next.longValue())
                    .atZone(DeliveryScheduleService.DELIVERY_ZONE).getHour());
        }

        @Test
        @DisplayName("a scheduled day that is today counts as the next delivery")
        void todayCounts() {
            Map<String, String> schedule = Map.of("monday", "true", "wednesday", "true");

            Integer next = DeliveryScheduleService.nextDeliveryAt(schedule, 0,
                    ist("2026-11-02T10:00:00"));

            assertEquals(MONDAY, dateOf(next));
        }

        @Test
        @DisplayName("Sunday is not a delivery day when it is absent from the schedule")
        void sundayIsSkipped() {
            Map<String, String> schedule = new LinkedHashMap<>();
            for (String day : new String[] { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" }) {
                schedule.put(day, "true");
            }

            Integer next = DeliveryScheduleService.nextDeliveryAt(schedule, 0,
                    ist("2026-11-08T12:00:00"));

            assertEquals(LocalDate.of(2026, 11, 9), dateOf(next));
        }

        @Test
        @DisplayName("a future subscription start is respected instead of today")
        void futureStartWins() {
            Map<String, String> schedule = Map.of("monday", "true");
            Instant start = ist("2026-11-10T09:00:00"); // Tuesday

            Integer next = DeliveryScheduleService.nextDeliveryAt(schedule, (int) start.getEpochSecond(),
                    ist("2026-11-03T09:00:00"));

            assertEquals(LocalDate.of(2026, 11, 16), dateOf(next));
        }

        @Test
        @DisplayName("no schedule means no date at all")
        void emptyScheduleHasNoDate() {
            assertNull(DeliveryScheduleService.nextDeliveryAt(Map.of(), 0, ist("2026-11-04T10:00:00")));
            assertNull(DeliveryScheduleService.nextDeliveryAt(null, 0, ist("2026-11-04T10:00:00")));
        }

        @Test
        @DisplayName("delivery days are exposed in ISO order")
        void deliveryDaysAreExposed() {
            Set<DayOfWeek> days = DeliveryScheduleService.deliveryDays(Map.of("friday", "x", "monday", "y"));

            assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), days);
        }
    }

    @Nested
    @DisplayName("canPromiseDelivery")
    class CanPromiseDelivery {

        @Test
        @DisplayName("active and future subscriptions may publish a date")
        void deliverableStatuses() {
            assertTrue(DeliveryScheduleService.canPromiseDelivery("active"));
            assertTrue(DeliveryScheduleService.canPromiseDelivery("ACTIVE"));
            assertTrue(DeliveryScheduleService.canPromiseDelivery("non_renewing"));
            assertTrue(DeliveryScheduleService.canPromiseDelivery("future"));
            assertTrue(DeliveryScheduleService.canPromiseDelivery(null));
        }

        @Test
        @DisplayName("paused and cancelled subscriptions never publish a date")
        void nonDeliverableStatuses() {
            assertFalse(DeliveryScheduleService.canPromiseDelivery("paused"));
            assertFalse(DeliveryScheduleService.canPromiseDelivery("cancelled"));
        }
    }
}
