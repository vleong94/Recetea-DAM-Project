package com.recetea.core.shared.domain.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("RelativeTimeFormatter — human-readable relative timestamps")
class RelativeTimeFormatterTest {

    // Message providers that echo key and count — no I18n dependency needed.
    private static final java.util.function.Function<String, String>           SIMPLE     = key -> key;
    private static final java.util.function.BiFunction<String, Long, String>  WITH_COUNT = (key, n) -> key + ":" + n;

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("null date returns empty string")
    void format_ShouldReturnEmpty_WhenDateIsNull() {
        assertEquals("", RelativeTimeFormatter.format(null, SIMPLE, WITH_COUNT));
    }

    @Test
    @DisplayName("date 30 seconds ago returns time.now key")
    void format_ShouldReturnNow_WhenLessThanOneMinute() {
        LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);
        assertEquals("time.now", RelativeTimeFormatter.format(thirtySecondsAgo, SIMPLE, WITH_COUNT));
    }

    // ── Minutes bucket (1 – 59) ───────────────────────────────────────────────

    @Test
    @DisplayName("date 5 minutes ago returns time.ago.minutes key with count 5")
    void format_ShouldReturnMinutes_WhenFiveMinutesAgo() {
        LocalDateTime date = LocalDateTime.now().minusMinutes(5);
        assertEquals("time.ago.minutes:5", RelativeTimeFormatter.format(date, SIMPLE, WITH_COUNT));
    }

    @Test
    @DisplayName("date 45 minutes ago returns time.ago.minutes key with count 45")
    void format_ShouldReturnMinutes_WhenFortyFiveMinutesAgo() {
        LocalDateTime date = LocalDateTime.now().minusMinutes(45);
        assertEquals("time.ago.minutes:45", RelativeTimeFormatter.format(date, SIMPLE, WITH_COUNT));
    }

    // ── Hours bucket (1 – 23) ─────────────────────────────────────────────────

    @Test
    @DisplayName("date 3 hours ago returns time.ago.hours key with count 3")
    void format_ShouldReturnHours_WhenThreeHoursAgo() {
        LocalDateTime date = LocalDateTime.now().minusHours(3);
        assertEquals("time.ago.hours:3", RelativeTimeFormatter.format(date, SIMPLE, WITH_COUNT));
    }

    @Test
    @DisplayName("date 23 hours ago returns time.ago.hours key with count 23")
    void format_ShouldReturnHours_WhenTwentyThreeHoursAgo() {
        LocalDateTime date = LocalDateTime.now().minusHours(23);
        assertEquals("time.ago.hours:23", RelativeTimeFormatter.format(date, SIMPLE, WITH_COUNT));
    }

    // ── Days bucket (1 – 29) ──────────────────────────────────────────────────

    @Test
    @DisplayName("date 15 days ago returns time.ago.days key with count 15")
    void format_ShouldReturnDays_WhenFifteenDaysAgo() {
        LocalDateTime date = LocalDateTime.now().minusDays(15);
        assertEquals("time.ago.days:15", RelativeTimeFormatter.format(date, SIMPLE, WITH_COUNT));
    }

    // ── Months bucket (1 – 11) ────────────────────────────────────────────────

    @Test
    @DisplayName("date 6 months ago returns time.ago.months key with count 6")
    void format_ShouldReturnMonths_WhenSixMonthsAgo() {
        LocalDateTime date = LocalDateTime.now().minusDays(6 * 30);
        assertEquals("time.ago.months:6", RelativeTimeFormatter.format(date, SIMPLE, WITH_COUNT));
    }

    // ── Years bucket ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("date 2 years ago returns time.ago.years key with count 2")
    void format_ShouldReturnYears_WhenTwoYearsAgo() {
        LocalDateTime date = LocalDateTime.now().minusDays(2 * 365);
        assertEquals("time.ago.years:2", RelativeTimeFormatter.format(date, SIMPLE, WITH_COUNT));
    }
}
